package com.salofresh.service.impl.approval;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salofresh.common.enums.ApprovalStatus;
import com.salofresh.common.enums.PayoutStatus;
import com.salofresh.dto.admin.SuspendSalonRequest;
import com.salofresh.dto.admin.SuspendUserRequest;
import com.salofresh.dto.approval.ApprovalRequestResponse;
import com.salofresh.dto.approval.CreateApprovalRequestRequest;
import com.salofresh.dto.approval.RejectApprovalRequestRequest;
import com.salofresh.dto.payment.RefundRequest;
import com.salofresh.entity.ApprovalRequest;
import com.salofresh.entity.SalonPayout;
import com.salofresh.entity.User;
import com.salofresh.exception.BadRequestException;
import com.salofresh.exception.ForbiddenException;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.repository.ApprovalRequestRepository;
import com.salofresh.repository.SalonPayoutRepository;
import com.salofresh.repository.UserRepository;
import com.salofresh.response.PagedResponse;
import com.salofresh.service.admin.AdminSalonService;
import com.salofresh.service.admin.AdminUserService;
import com.salofresh.service.approval.ApprovalRequestService;
import com.salofresh.service.payment.RefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Implements the maker-checker approval workflow. A maker admin queues an {@link ApprovalRequest}
 * via {@link #create}; a different checker (SUPER_ADMIN) reviews it and either rejects it or
 * approves it, which triggers execution of the underlying high-risk action.
 *
 * <p>Execution wiring per {@code ApprovalActionType}:
 * <ul>
 *   <li>SALON_SUSPENSION and USER_BAN are fully wired to the existing
 *       {@link AdminSalonService#suspend} / {@link AdminUserService#suspendUser} methods.</li>
 *   <li>LARGE_REFUND is fully wired to the existing {@link RefundService#initiateRefund}, which
 *       already creates the {@code Refund} record, calls the payment gateway, and updates the
 *       payment/wallet bookkeeping.</li>
 *   <li>PAYOUT_OVERRIDE is a best-effort, simplified execution: it directly overwrites the
 *       targeted {@code SalonPayout}'s amount/status via the repository, with no ledger
 *       recalculation. See the inline TODO for what a fuller implementation would need.</li>
 *   <li>OTHER has no generic executable target; approving it simply records the payload for a
 *       human admin to action manually and marks the request EXECUTED as an audit trail, not as
 *       proof any system state changed.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalRequestServiceImpl implements ApprovalRequestService {

    private static final String EXECUTION_FAILURE_PREFIX = "Approval approved but execution failed: ";

    private final ApprovalRequestRepository approvalRequestRepository;
    private final UserRepository userRepository;
    private final SalonPayoutRepository salonPayoutRepository;
    private final AdminSalonService adminSalonService;
    private final AdminUserService adminUserService;
    private final RefundService refundService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ApprovalRequestResponse create(Long requestedByUserId, CreateApprovalRequestRequest request) {
        User maker = findUser(requestedByUserId);
        validatePayloadJson(request.getPayloadJson());

        ApprovalRequest approval = ApprovalRequest.builder()
                .actionType(request.getActionType())
                .requestedBy(maker)
                .payload(request.getPayloadJson())
                .relatedEntityType(request.getRelatedEntityType())
                .relatedEntityId(request.getRelatedEntityId())
                .reason(request.getReason())
                .status(ApprovalStatus.PENDING)
                .build();

        ApprovalRequest saved = approvalRequestRepository.save(approval);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ApprovalRequestResponse> listPending(Pageable pageable) {
        Page<ApprovalRequest> page = approvalRequestRepository.findAllByStatusOrderByCreatedAtDesc(ApprovalStatus.PENDING, pageable);
        return PagedResponse.from(page, page.getContent().stream().map(this::toResponse).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ApprovalRequestResponse> listAll(ApprovalStatus status, Pageable pageable) {
        Page<ApprovalRequest> page = status != null
                ? approvalRequestRepository.findAllByStatusOrderByCreatedAtDesc(status, pageable)
                : approvalRequestRepository.findAllByOrderByCreatedAtDesc(pageable);
        return PagedResponse.from(page, page.getContent().stream().map(this::toResponse).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ApprovalRequestResponse> listMine(Long requestedByUserId, Pageable pageable) {
        Page<ApprovalRequest> page = approvalRequestRepository.findAllByRequestedByIdOrderByCreatedAtDesc(requestedByUserId, pageable);
        return PagedResponse.from(page, page.getContent().stream().map(this::toResponse).toList());
    }

    @Override
    @Transactional
    public ApprovalRequestResponse approveAndExecute(Long checkerUserId, Long approvalId) {
        ApprovalRequest approval = findApproval(approvalId);
        guardPendingAndDifferentChecker(approval, checkerUserId);

        switch (approval.getActionType()) {
            case SALON_SUSPENSION -> executeSalonSuspension(approval);
            case USER_BAN -> executeUserBan(approval);
            case LARGE_REFUND -> executeLargeRefund(approval, checkerUserId);
            case PAYOUT_OVERRIDE -> executePayoutOverride(approval);
            case OTHER -> executeOther(approval);
        }

        // Only reached if execution above did not throw. If it threw, the exception propagates
        // out of this @Transactional method and the whole transaction rolls back, leaving the
        // approval request PENDING (and any partial side effects of the failed execution undone)
        // so an admin can inspect and retry.
        User checker = findUser(checkerUserId);
        approval.setStatus(ApprovalStatus.EXECUTED);
        approval.setDecidedBy(checker);
        approval.setDecidedAt(Instant.now());
        ApprovalRequest saved = approvalRequestRepository.save(approval);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public ApprovalRequestResponse reject(Long checkerUserId, Long approvalId, RejectApprovalRequestRequest request) {
        ApprovalRequest approval = findApproval(approvalId);
        guardPendingAndDifferentChecker(approval, checkerUserId);

        User checker = findUser(checkerUserId);
        approval.setStatus(ApprovalStatus.REJECTED);
        approval.setRejectionReason(request.getRejectionReason());
        approval.setDecidedBy(checker);
        approval.setDecidedAt(Instant.now());

        ApprovalRequest saved = approvalRequestRepository.save(approval);
        return toResponse(saved);
    }

    private void guardPendingAndDifferentChecker(ApprovalRequest approval, Long checkerUserId) {
        if (approval.getStatus() != ApprovalStatus.PENDING) {
            throw new BadRequestException("Only PENDING approval requests can be decided (current status: " + approval.getStatus() + ")");
        }
        if (approval.getRequestedBy() != null && approval.getRequestedBy().getId().equals(checkerUserId)) {
            throw new ForbiddenException("You cannot approve your own request");
        }
    }

    // ---- Execution branches, one try/catch each so a failure in one action type never leaves
    // ---- the approval record in a corrupted (partially-executed) state. ----

    private void executeSalonSuspension(ApprovalRequest approval) {
        try {
            JsonNode node = objectMapper.readTree(approval.getPayload());
            if (!node.hasNonNull("salonId")) {
                throw new BadRequestException("payload.salonId is required for SALON_SUSPENSION");
            }
            Long salonId = node.get("salonId").asLong();
            String reason = resolveReason(node, approval);
            adminSalonService.suspend(salonId, SuspendSalonRequest.builder().reason(reason).build());
        } catch (Exception e) {
            log.error("Failed to execute SALON_SUSPENSION for approval {}: {}", approval.getId(), e.getMessage(), e);
            throw new BadRequestException(EXECUTION_FAILURE_PREFIX + e.getMessage());
        }
    }

    private void executeUserBan(ApprovalRequest approval) {
        try {
            JsonNode node = objectMapper.readTree(approval.getPayload());
            if (!node.hasNonNull("userId")) {
                throw new BadRequestException("payload.userId is required for USER_BAN");
            }
            Long userId = node.get("userId").asLong();
            String reason = resolveReason(node, approval);
            adminUserService.suspendUser(userId, SuspendUserRequest.builder().reason(reason).build());
        } catch (Exception e) {
            log.error("Failed to execute USER_BAN for approval {}: {}", approval.getId(), e.getMessage(), e);
            throw new BadRequestException(EXECUTION_FAILURE_PREFIX + e.getMessage());
        }
    }

    private void executeLargeRefund(ApprovalRequest approval, Long checkerUserId) {
        try {
            JsonNode node = objectMapper.readTree(approval.getPayload());
            if (!node.hasNonNull("paymentId")) {
                throw new BadRequestException("payload.paymentId is required for LARGE_REFUND");
            }
            Long paymentId = node.get("paymentId").asLong();
            BigDecimal amount = node.hasNonNull("amount") ? new BigDecimal(node.get("amount").asText()) : null;
            String reason = resolveReason(node, approval);

            // Fully wired: reuses the existing refund pipeline (creates the Refund record, calls
            // the configured PaymentGateway.refund(...), and updates payment/wallet bookkeeping)
            // instead of re-implementing any of that here. The checker is passed as the acting
            // user; RefundServiceImpl's admin check reads the authenticated principal from the
            // security context, which is the checker making this HTTP call.
            refundService.initiateRefund(paymentId, RefundRequest.builder().amount(amount).reason(reason).build(), checkerUserId);
        } catch (Exception e) {
            log.error("Failed to execute LARGE_REFUND for approval {}: {}", approval.getId(), e.getMessage(), e);
            throw new BadRequestException(EXECUTION_FAILURE_PREFIX + e.getMessage());
        }
    }

    private void executePayoutOverride(ApprovalRequest approval) {
        try {
            JsonNode node = objectMapper.readTree(approval.getPayload());
            if (!node.hasNonNull("payoutId")) {
                throw new BadRequestException("payload.payoutId is required for PAYOUT_OVERRIDE");
            }
            Long payoutId = node.get("payoutId").asLong();
            SalonPayout payout = salonPayoutRepository.findById(payoutId)
                    .orElseThrow(() -> new ResourceNotFoundException("SalonPayout", "id", payoutId));

            // Best-effort/simplified execution, as explicitly scoped for this task: we overwrite
            // the payout's amount/status directly via the repository. This does NOT recompute
            // gross revenue/commission or emit any ledger adjustment entry.
            // TODO: if a dedicated payout-adjustment/ledger capability is added later, route
            // PAYOUT_OVERRIDE through that instead of mutating SalonPayout directly.
            boolean changed = false;
            if (node.hasNonNull("netPayoutAmount")) {
                payout.setNetPayoutAmount(new BigDecimal(node.get("netPayoutAmount").asText()));
                changed = true;
            }
            if (node.hasNonNull("status")) {
                payout.setStatus(PayoutStatus.valueOf(node.get("status").asText()));
                changed = true;
            }
            if (!changed) {
                throw new BadRequestException("payload must include at least one of netPayoutAmount or status for PAYOUT_OVERRIDE");
            }
            salonPayoutRepository.save(payout);
        } catch (Exception e) {
            log.error("Failed to execute PAYOUT_OVERRIDE for approval {}: {}", approval.getId(), e.getMessage(), e);
            throw new BadRequestException(EXECUTION_FAILURE_PREFIX + e.getMessage());
        }
    }

    private void executeOther(ApprovalRequest approval) {
        // No generic system action exists for OTHER. Rather than pretend to execute something
        // undefined, we simply log the payload so a human admin can carry out the actual action
        // out-of-band; the approval record itself is the audit trail that it was reviewed and
        // approved. This intentionally never throws, so OTHER approvals always succeed.
        log.info("Approval {} (OTHER) approved; payload recorded for manual execution: {}",
                approval.getId(), approval.getPayload());
    }

    private String resolveReason(JsonNode node, ApprovalRequest approval) {
        if (node.hasNonNull("reason") && !node.get("reason").asText().isBlank()) {
            return node.get("reason").asText();
        }
        if (approval.getReason() != null && !approval.getReason().isBlank()) {
            return approval.getReason();
        }
        return "Approved via maker-checker workflow (approval #" + approval.getId() + ")";
    }

    private void validatePayloadJson(String json) {
        try {
            objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("payloadJson must be valid JSON: " + e.getOriginalMessage());
        }
    }

    private ApprovalRequest findApproval(Long id) {
        return approvalRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ApprovalRequest", "id", id));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    private ApprovalRequestResponse toResponse(ApprovalRequest approval) {
        User requestedBy = approval.getRequestedBy();
        User decidedBy = approval.getDecidedBy();
        return ApprovalRequestResponse.builder()
                .id(approval.getId())
                .actionType(approval.getActionType())
                .requestedById(requestedBy != null ? requestedBy.getId() : null)
                .requestedByName(requestedBy != null ? requestedBy.getFullName() : null)
                .payload(approval.getPayload())
                .relatedEntityType(approval.getRelatedEntityType())
                .relatedEntityId(approval.getRelatedEntityId())
                .reason(approval.getReason())
                .status(approval.getStatus())
                .decidedById(decidedBy != null ? decidedBy.getId() : null)
                .decidedByName(decidedBy != null ? decidedBy.getFullName() : null)
                .decidedAt(approval.getDecidedAt())
                .rejectionReason(approval.getRejectionReason())
                .createdAt(approval.getCreatedAt())
                .build();
    }
}
