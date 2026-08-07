package com.salofresh.service.impl.marketing;

import com.salofresh.common.enums.CampaignAudience;
import com.salofresh.common.enums.CampaignRecipientStatus;
import com.salofresh.common.enums.CampaignStatus;
import com.salofresh.common.enums.CustomerNoteTag;
import com.salofresh.common.enums.NotificationChannel;
import com.salofresh.common.enums.NotificationType;
import com.salofresh.dto.marketing.CampaignRecipientResponse;
import com.salofresh.dto.marketing.CampaignResponse;
import com.salofresh.dto.marketing.CreateCampaignRequest;
import com.salofresh.email.EmailService;
import com.salofresh.email.EmailTemplateBuilder;
import com.salofresh.entity.Appointment;
import com.salofresh.entity.CampaignRecipient;
import com.salofresh.entity.CustomerNote;
import com.salofresh.entity.MarketingCampaign;
import com.salofresh.entity.Salon;
import com.salofresh.entity.User;
import com.salofresh.exception.BadRequestException;
import com.salofresh.exception.ForbiddenException;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.mapper.marketing.CampaignMapper;
import com.salofresh.notification.NotificationService;
import com.salofresh.push.PushNotificationService;
import com.salofresh.repository.AppointmentRepository;
import com.salofresh.repository.CampaignRecipientRepository;
import com.salofresh.repository.CustomerNoteRepository;
import com.salofresh.repository.MarketingCampaignRepository;
import com.salofresh.repository.SalonRepository;
import com.salofresh.repository.UserRepository;
import com.salofresh.response.PagedResponse;
import com.salofresh.service.marketing.MarketingCampaignService;
import com.salofresh.sms.SmsService;
import com.salofresh.whatsapp.WhatsAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Implements marketing broadcast creation, dispatch and lifecycle management.
 *
 * <p>Channel dispatch notes: EmailService/SmsService/WhatsAppService in this codebase are
 * {@code @Async} and swallow their own delivery failures (they log the outcome into their own
 * *_logs tables and never throw back to the caller). That means a CampaignRecipient marked SENT
 * here reflects "the dispatch call was made with a usable email/phone on file", not a confirmed
 * final delivery receipt. A production system would want a webhook/poll-back against those log
 * tables to reconcile true delivery status. PushNotificationService is documented as
 * best-effort/never-throwing for the same reason. IN_APP (NotificationService.createAndDispatch)
 * is synchronous and will surface genuine failures.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MarketingCampaignServiceImpl implements MarketingCampaignService {

    private static final int INACTIVE_THRESHOLD_DAYS = 90;

    private final MarketingCampaignRepository marketingCampaignRepository;
    private final CampaignRecipientRepository campaignRecipientRepository;
    private final SalonRepository salonRepository;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final CustomerNoteRepository customerNoteRepository;
    private final CampaignMapper campaignMapper;

    private final EmailService emailService;
    private final EmailTemplateBuilder emailTemplateBuilder;
    private final SmsService smsService;
    private final WhatsAppService whatsAppService;
    private final PushNotificationService pushNotificationService;
    private final NotificationService notificationService;

    @Override
    public CampaignResponse create(Long ownerUserId, Long salonId, CreateCampaignRequest request) {
        Salon salon = getOwnedSalon(ownerUserId, salonId);

        if (request.getTargetAudience() == CampaignAudience.CUSTOM
                && (request.getCustomCustomerIds() == null || request.getCustomCustomerIds().isEmpty())) {
            throw new BadRequestException("customCustomerIds is required when targetAudience is CUSTOM");
        }

        List<User> recipients = resolveAudience(salon, request.getTargetAudience(), request.getCustomCustomerIds());
        if (recipients.isEmpty()) {
            throw new BadRequestException("No customers matched the selected target audience");
        }

        User createdBy = userRepository.findById(ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", ownerUserId));

        boolean sendImmediately = request.getScheduledAt() == null || !request.getScheduledAt().isAfter(Instant.now());

        MarketingCampaign campaign = MarketingCampaign.builder()
                .salon(salon)
                .title(request.getTitle())
                .message(request.getMessage())
                .channel(request.getChannel())
                .targetAudience(request.getTargetAudience())
                .scheduledAt(sendImmediately ? null : request.getScheduledAt())
                .status(sendImmediately ? CampaignStatus.DRAFT : CampaignStatus.SCHEDULED)
                .createdByUser(createdBy)
                .build();
        campaign = marketingCampaignRepository.save(campaign);
        final MarketingCampaign savedCampaign = campaign;

        List<CampaignRecipient> campaignRecipients = recipients.stream()
                .map(customer -> CampaignRecipient.builder()
                        .campaign(savedCampaign)
                        .customer(customer)
                        .status(CampaignRecipientStatus.PENDING)
                        .build())
                .toList();
        campaignRecipientRepository.saveAll(campaignRecipients);

        if (sendImmediately) {
            return dispatch(savedCampaign);
        }
        return toResponseWithCounts(campaign);
    }

    @Override
    public CampaignResponse sendNow(Long ownerUserId, Long campaignId) {
        MarketingCampaign campaign = getOwnedCampaign(ownerUserId, campaignId);
        if (campaign.getStatus() != CampaignStatus.DRAFT && campaign.getStatus() != CampaignStatus.SCHEDULED) {
            throw new BadRequestException("Only DRAFT or SCHEDULED campaigns can be sent");
        }
        return dispatch(campaign);
    }

    @Override
    public CampaignResponse cancel(Long ownerUserId, Long campaignId) {
        MarketingCampaign campaign = getOwnedCampaign(ownerUserId, campaignId);
        if (campaign.getStatus() != CampaignStatus.DRAFT && campaign.getStatus() != CampaignStatus.SCHEDULED) {
            throw new BadRequestException("Only DRAFT or SCHEDULED campaigns can be cancelled");
        }
        campaign.setStatus(CampaignStatus.CANCELLED);
        campaign = marketingCampaignRepository.save(campaign);
        return toResponseWithCounts(campaign);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<CampaignResponse> listForSalon(Long ownerUserId, Long salonId, Pageable pageable) {
        Salon salon = getOwnedSalon(ownerUserId, salonId);
        Page<MarketingCampaign> page = marketingCampaignRepository.findAllBySalonIdOrderByCreatedAtDesc(salon.getId(), pageable);
        List<CampaignResponse> content = page.getContent().stream().map(this::toResponseWithCounts).toList();
        return PagedResponse.from(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public CampaignResponse getById(Long ownerUserId, Long campaignId) {
        MarketingCampaign campaign = getOwnedCampaign(ownerUserId, campaignId);
        return toResponseWithCounts(campaign);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<CampaignRecipientResponse> listRecipients(Long ownerUserId, Long campaignId, Pageable pageable) {
        MarketingCampaign campaign = getOwnedCampaign(ownerUserId, campaignId);
        Page<CampaignRecipient> page = campaignRecipientRepository.findAllByCampaignId(campaign.getId(), pageable);
        List<CampaignRecipientResponse> content = page.getContent().stream().map(campaignMapper::toRecipientResponse).toList();
        return PagedResponse.from(page, content);
    }

    // ---------------------------------------------------------------- dispatch

    private CampaignResponse dispatch(MarketingCampaign campaign) {
        campaign.setStatus(CampaignStatus.SENDING);
        campaign = marketingCampaignRepository.save(campaign);

        List<CampaignRecipient> pending =
                campaignRecipientRepository.findAllByCampaignIdAndStatus(campaign.getId(), CampaignRecipientStatus.PENDING);

        int failureCount = 0;
        for (CampaignRecipient recipient : pending) {
            try {
                dispatchToRecipient(campaign, recipient);
                recipient.setStatus(CampaignRecipientStatus.SENT);
                recipient.setSentAt(Instant.now());
                recipient.setErrorMessage(null);
            } catch (Exception ex) {
                failureCount++;
                recipient.setStatus(CampaignRecipientStatus.FAILED);
                recipient.setErrorMessage(truncateErrorMessage(ex.getMessage()));
                log.warn("Failed to dispatch campaign {} to recipient {}: {}",
                        campaign.getId(), recipient.getId(), ex.getMessage());
            }
            campaignRecipientRepository.save(recipient);
        }

        boolean allFailed = !pending.isEmpty() && failureCount == pending.size();
        campaign.setStatus(allFailed ? CampaignStatus.FAILED : CampaignStatus.SENT);
        campaign.setSentAt(Instant.now());
        campaign = marketingCampaignRepository.save(campaign);

        return toResponseWithCounts(campaign);
    }

    private void dispatchToRecipient(MarketingCampaign campaign, CampaignRecipient recipient) {
        User customer = recipient.getCustomer();
        NotificationChannel channel = campaign.getChannel();
        switch (channel) {
            case EMAIL -> {
                String email = customer.getEmail();
                if (email == null || email.isBlank()) {
                    throw new IllegalStateException("Customer has no email address on file");
                }
                emailService.sendHtmlEmail(email, campaign.getTitle(),
                        emailTemplateBuilder.buildMarketingEmail(customer.getFullName(), campaign.getTitle(), campaign.getMessage()));
            }
            case SMS -> {
                String phone = customer.getPhone();
                if (phone == null || phone.isBlank()) {
                    throw new IllegalStateException("Customer has no phone number on file");
                }
                smsService.sendSms(phone, campaign.getMessage());
            }
            case WHATSAPP -> {
                String phone = customer.getPhone();
                if (phone == null || phone.isBlank()) {
                    throw new IllegalStateException("Customer has no phone number on file");
                }
                whatsAppService.sendMessage(phone, campaign.getMessage());
            }
            case PUSH -> pushNotificationService.sendToUser(customer, campaign.getTitle(), campaign.getMessage(), Map.of());
            case IN_APP -> notificationService.createAndDispatch(customer, NotificationType.GENERIC, NotificationChannel.IN_APP,
                    campaign.getTitle(), campaign.getMessage(), String.valueOf(campaign.getId()), "MarketingCampaign");
            default -> throw new IllegalStateException("Unsupported campaign channel: " + channel);
        }
    }

    private String truncateErrorMessage(String message) {
        if (message == null) {
            return "Unknown error";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    // ---------------------------------------------------------------- audience resolution

    private List<User> resolveAudience(Salon salon, CampaignAudience audience, List<Long> customCustomerIds) {
        return switch (audience) {
            case ALL_CUSTOMERS -> resolveAllCustomers(salon.getId());
            case VIP_CUSTOMERS -> resolveVipCustomers(salon.getId());
            case INACTIVE_CUSTOMERS -> resolveInactiveCustomers(salon.getId());
            case CUSTOM -> resolveCustomCustomers(customCustomerIds);
        };
    }

    /**
     * ALL_CUSTOMERS = every distinct customer who has ever booked at this salon, derived from
     * AppointmentRepository (no new repository method needed).
     */
    private List<User> resolveAllCustomers(Long salonId) {
        List<Appointment> appointments = appointmentRepository.findAllBySalonId(salonId, Pageable.unpaged()).getContent();
        Map<Long, User> distinct = new LinkedHashMap<>();
        for (Appointment appointment : appointments) {
            User customer = appointment.getCustomer();
            if (customer != null) {
                distinct.putIfAbsent(customer.getId(), customer);
            }
        }
        return List.copyOf(distinct.values());
    }

    /**
     * VIP_CUSTOMERS: CustomerNoteRepository is present in this worktree, but it only exposes
     * per-customer lookup methods (findAllBySalonIdAndCustomerId..., findByIdAndSalonId...),
     * not a bulk "all notes for a salon" query. Rather than add a new derived query method
     * (not pre-authorized), we use the inherited JpaRepository#findAll() and filter to this
     * salon's VIP-tagged notes in memory — the same "acceptable at this scale" pattern used by
     * CampaignDispatchScheduler for locating due campaigns.
     */
    private List<User> resolveVipCustomers(Long salonId) {
        Map<Long, User> distinct = new LinkedHashMap<>();
        for (CustomerNote note : customerNoteRepository.findAll()) {
            if (note.isDeleted() || note.getTag() != CustomerNoteTag.VIP) {
                continue;
            }
            if (note.getSalon() == null || !salonId.equals(note.getSalon().getId())) {
                continue;
            }
            User customer = note.getCustomer();
            if (customer != null) {
                distinct.putIfAbsent(customer.getId(), customer);
            }
        }
        return List.copyOf(distinct.values());
    }

    /**
     * INACTIVE_CUSTOMERS = customers who booked at this salon before but have no appointment
     * in the last {@value #INACTIVE_THRESHOLD_DAYS} days.
     */
    private List<User> resolveInactiveCustomers(Long salonId) {
        List<Appointment> appointments = appointmentRepository.findAllBySalonId(salonId, Pageable.unpaged()).getContent();
        Map<Long, User> customerById = new LinkedHashMap<>();
        Map<Long, LocalDate> lastAppointmentDateByCustomer = new HashMap<>();
        for (Appointment appointment : appointments) {
            User customer = appointment.getCustomer();
            if (customer == null) {
                continue;
            }
            customerById.putIfAbsent(customer.getId(), customer);
            LocalDate appointmentDate = appointment.getAppointmentDate();
            lastAppointmentDateByCustomer.merge(customer.getId(), appointmentDate,
                    (existing, candidate) -> candidate.isAfter(existing) ? candidate : existing);
        }
        LocalDate cutoff = LocalDate.now().minusDays(INACTIVE_THRESHOLD_DAYS);
        return customerById.values().stream()
                .filter(customer -> {
                    LocalDate lastDate = lastAppointmentDateByCustomer.get(customer.getId());
                    return lastDate != null && lastDate.isBefore(cutoff);
                })
                .toList();
    }

    /**
     * CUSTOM = an explicit list of customer ids passed in the create-campaign request.
     */
    private List<User> resolveCustomCustomers(List<Long> customCustomerIds) {
        Set<Long> requestedIds = Set.copyOf(customCustomerIds);
        List<User> users = userRepository.findAllById(requestedIds);
        if (users.isEmpty()) {
            throw new BadRequestException("None of the given customCustomerIds could be resolved to a valid customer");
        }
        return users;
    }

    // ---------------------------------------------------------------- ownership + mapping helpers

    private Salon getOwnedSalon(Long ownerUserId, Long salonId) {
        Salon salon = salonRepository.findByIdAndDeletedFalse(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon", "id", salonId));
        verifyOwnership(salon, ownerUserId);
        return salon;
    }

    private MarketingCampaign getOwnedCampaign(Long ownerUserId, Long campaignId) {
        MarketingCampaign campaign = marketingCampaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("MarketingCampaign", "id", campaignId));
        verifyOwnership(campaign.getSalon(), ownerUserId);
        return campaign;
    }

    private void verifyOwnership(Salon salon, Long ownerUserId) {
        if (salon.getOwner() == null || salon.getOwner().getUser() == null
                || !Objects.equals(salon.getOwner().getUser().getId(), ownerUserId)) {
            throw new ForbiddenException("You do not have permission to manage campaigns for this salon");
        }
    }

    private CampaignResponse toResponseWithCounts(MarketingCampaign campaign) {
        CampaignResponse response = campaignMapper.toResponse(campaign);
        long sentCount = campaignRecipientRepository.countByCampaignIdAndStatus(campaign.getId(), CampaignRecipientStatus.SENT);
        long failedCount = campaignRecipientRepository.countByCampaignIdAndStatus(campaign.getId(), CampaignRecipientStatus.FAILED);
        long pendingCount = campaignRecipientRepository.countByCampaignIdAndStatus(campaign.getId(), CampaignRecipientStatus.PENDING);
        response.setSentCount(sentCount);
        response.setFailedCount(failedCount);
        response.setRecipientCount(sentCount + failedCount + pendingCount);
        return response;
    }
}
