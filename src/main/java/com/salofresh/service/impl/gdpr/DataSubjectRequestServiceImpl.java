package com.salofresh.service.impl.gdpr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salofresh.common.enums.AccountStatus;
import com.salofresh.common.enums.DataRequestStatus;
import com.salofresh.common.enums.DataRequestType;
import com.salofresh.config.AppProperties;
import com.salofresh.dto.gdpr.CreateDataRequestRequest;
import com.salofresh.dto.gdpr.DataSubjectRequestResponse;
import com.salofresh.dto.gdpr.RejectDataRequestRequest;
import com.salofresh.entity.Appointment;
import com.salofresh.entity.Customer;
import com.salofresh.entity.DataSubjectRequest;
import com.salofresh.entity.Payment;
import com.salofresh.entity.Review;
import com.salofresh.entity.User;
import com.salofresh.exception.BadRequestException;
import com.salofresh.exception.ForbiddenException;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.exception.ConflictException;
import com.salofresh.exception.FileStorageException;
import com.salofresh.repository.AppointmentRepository;
import com.salofresh.repository.CustomerRepository;
import com.salofresh.repository.DataSubjectRequestRepository;
import com.salofresh.repository.PaymentRepository;
import com.salofresh.repository.ReviewRepository;
import com.salofresh.repository.UserRepository;
import com.salofresh.response.PagedResponse;
import com.salofresh.service.gdpr.DataSubjectRequestService;
import com.salofresh.util.RandomCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DataSubjectRequestServiceImpl implements DataSubjectRequestService {

    /**
     * Sub-directory (under {@code app.file.upload-dir}) where generated GDPR/DPDP export
     * archives are written. There is no reusable storage abstraction that accepts an arbitrary
     * in-memory blob to persist ({@link com.salofresh.file.FileStorageService#store} only accepts
     * a {@link org.springframework.web.multipart.MultipartFile}, which this export data never is),
     * so per the task's documented fallback we write the JSON export directly to the same
     * upload-dir/base-url pair the file module already uses, and store the resulting URL string
     * in {@code exportFileUrl} exactly as {@code LocalFileStorageServiceImpl} does.
     */
    private static final String EXPORT_SUB_DIRECTORY = "gdpr-exports";

    private static final List<DataRequestStatus> OPEN_STATUSES = List.of(DataRequestStatus.PENDING, DataRequestStatus.PROCESSING);

    private final DataSubjectRequestRepository dataSubjectRequestRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final AppointmentRepository appointmentRepository;
    private final PaymentRepository paymentRepository;
    private final ReviewRepository reviewRepository;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public DataSubjectRequestResponse create(Long userId, CreateDataRequestRequest request) {
        if (dataSubjectRequestRepository.existsByUserIdAndRequestTypeAndStatusIn(userId, request.getRequestType(), OPEN_STATUSES)) {
            throw new ConflictException("A " + request.getRequestType() + " request is already pending or being processed");
        }
        User user = findUser(userId);

        DataSubjectRequest saved = dataSubjectRequestRepository.save(DataSubjectRequest.builder()
                .user(user)
                .requestType(request.getRequestType())
                .status(DataRequestStatus.PENDING)
                .requestedAt(Instant.now())
                .build());

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<DataSubjectRequestResponse> listMine(Long userId, Pageable pageable) {
        Page<DataSubjectRequest> page = dataSubjectRequestRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PagedResponse.from(page, page.getContent().stream().map(this::toResponse).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DataSubjectRequestResponse getMine(Long userId, Long requestId) {
        DataSubjectRequest dataSubjectRequest = findRequest(requestId);
        if (dataSubjectRequest.getUser() == null || !dataSubjectRequest.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You do not have access to this data subject request");
        }
        return toResponse(dataSubjectRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<DataSubjectRequestResponse> listAll(DataRequestStatus status, Pageable pageable) {
        Page<DataSubjectRequest> page = status == null
                ? dataSubjectRequestRepository.findAllByOrderByCreatedAtDesc(pageable)
                : dataSubjectRequestRepository.findAllByStatusOrderByCreatedAtDesc(status, pageable);
        return PagedResponse.from(page, page.getContent().stream().map(this::toResponse).toList());
    }

    @Override
    @Transactional
    public DataSubjectRequestResponse processExport(Long adminUserId, Long requestId) {
        DataSubjectRequest dataSubjectRequest = findRequest(requestId);
        if (dataSubjectRequest.getRequestType() != DataRequestType.EXPORT || dataSubjectRequest.getStatus() != DataRequestStatus.PENDING) {
            throw new BadRequestException("Request must be an EXPORT request in PENDING status to be processed");
        }
        User admin = findUser(adminUserId);

        dataSubjectRequest.setStatus(DataRequestStatus.PROCESSING);
        dataSubjectRequestRepository.save(dataSubjectRequest);

        User dataSubject = dataSubjectRequest.getUser();
        Map<String, Object> exportPayload = buildExportPayload(dataSubject);
        String exportFileUrl = writeExportFile(dataSubject.getId(), exportPayload);

        dataSubjectRequest.setExportFileUrl(exportFileUrl);
        dataSubjectRequest.setStatus(DataRequestStatus.COMPLETED);
        dataSubjectRequest.setCompletedAt(Instant.now());
        dataSubjectRequest.setProcessedBy(admin);

        return toResponse(dataSubjectRequestRepository.save(dataSubjectRequest));
    }

    @Override
    @Transactional
    public DataSubjectRequestResponse processDeletion(Long adminUserId, Long requestId) {
        DataSubjectRequest dataSubjectRequest = findRequest(requestId);
        if (dataSubjectRequest.getRequestType() != DataRequestType.DELETE || dataSubjectRequest.getStatus() != DataRequestStatus.PENDING) {
            throw new BadRequestException("Request must be a DELETE request in PENDING status to be processed");
        }
        User admin = findUser(adminUserId);

        dataSubjectRequest.setStatus(DataRequestStatus.PROCESSING);
        dataSubjectRequestRepository.save(dataSubjectRequest);

        User dataSubject = dataSubjectRequest.getUser();
        softDeleteAndAnonymize(dataSubject);
        userRepository.save(dataSubject);

        dataSubjectRequest.setStatus(DataRequestStatus.COMPLETED);
        dataSubjectRequest.setCompletedAt(Instant.now());
        dataSubjectRequest.setProcessedBy(admin);

        return toResponse(dataSubjectRequestRepository.save(dataSubjectRequest));
    }

    @Override
    @Transactional
    public DataSubjectRequestResponse reject(Long adminUserId, Long requestId, RejectDataRequestRequest request) {
        DataSubjectRequest dataSubjectRequest = findRequest(requestId);
        User admin = findUser(adminUserId);

        dataSubjectRequest.setStatus(DataRequestStatus.REJECTED);
        dataSubjectRequest.setRejectionReason(request.getRejectionReason());
        dataSubjectRequest.setProcessedBy(admin);
        dataSubjectRequest.setCompletedAt(Instant.now());

        return toResponse(dataSubjectRequestRepository.save(dataSubjectRequest));
    }

    /**
     * Soft-deletes the account and anonymizes only the PII columns that are nullable in the
     * {@code users} table (see {@code V1__init_schema.sql}): {@code phone}, {@code password} and
     * {@code profile_image_url}. {@code email} and {@code first_name} are declared {@code NOT NULL}
     * so they are left untouched; {@link AccountStatus#DELETED} together with the inherited
     * {@code deleted} flag is what marks the account as gone for the rest of the system.
     */
    private void softDeleteAndAnonymize(User user) {
        user.setDeleted(true);
        user.setAccountStatus(AccountStatus.DELETED);
        user.setPhone(null);
        user.setPassword(null);
        user.setProfileImageUrl(null);
    }

    private Map<String, Object> buildExportPayload(User user) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", user.getId());
        profile.put("email", user.getEmail());
        profile.put("phone", user.getPhone());
        profile.put("firstName", user.getFirstName());
        profile.put("lastName", user.getLastName());
        profile.put("gender", user.getGender());
        profile.put("dateOfBirth", user.getDateOfBirth());
        profile.put("authProvider", user.getAuthProvider());
        profile.put("emailVerified", user.isEmailVerified());
        profile.put("phoneVerified", user.isPhoneVerified());
        profile.put("accountStatus", user.getAccountStatus());
        profile.put("referralCode", user.getReferralCode());
        profile.put("createdAt", user.getCreatedAt());

        Optional<Customer> customer = customerRepository.findByUserId(user.getId());
        Map<String, Object> customerProfile = customer.map(c -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("membershipLevel", c.getMembershipLevel());
            map.put("totalBookings", c.getTotalBookings());
            map.put("totalSpent", c.getTotalSpent());
            map.put("preferredSalonId", c.getPreferredSalonId());
            map.put("preferredEmployeeId", c.getPreferredEmployeeId());
            return map;
        }).orElse(null);

        List<Map<String, Object>> bookings = appointmentRepository.findAllByCustomerId(user.getId(), Pageable.unpaged())
                .getContent().stream().map(this::toAppointmentMap).toList();

        List<Map<String, Object>> payments = paymentRepository.findAllByUserId(user.getId(), Pageable.unpaged())
                .getContent().stream().map(this::toPaymentMap).toList();

        List<Map<String, Object>> reviews = reviewRepository.findAllByCustomerIdAndDeletedFalse(user.getId(), Pageable.unpaged())
                .getContent().stream().map(this::toReviewMap).toList();

        Map<String, Object> export = new LinkedHashMap<>();
        export.put("generatedAt", Instant.now());
        export.put("profile", profile);
        export.put("customerProfile", customerProfile);
        export.put("bookings", bookings);
        export.put("payments", payments);
        export.put("reviews", reviews);
        return export;
    }

    private Map<String, Object> toAppointmentMap(Appointment appointment) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", appointment.getId());
        map.put("bookingNumber", appointment.getBookingNumber());
        map.put("salonId", appointment.getSalon() != null ? appointment.getSalon().getId() : null);
        map.put("appointmentDate", appointment.getAppointmentDate());
        map.put("startTime", appointment.getStartTime());
        map.put("endTime", appointment.getEndTime());
        map.put("status", appointment.getStatus());
        map.put("finalAmount", appointment.getFinalAmount());
        return map;
    }

    private Map<String, Object> toPaymentMap(Payment payment) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", payment.getId());
        map.put("amount", payment.getAmount());
        map.put("currency", payment.getCurrency());
        map.put("paymentMethod", payment.getPaymentMethod());
        map.put("paymentStatus", payment.getPaymentStatus());
        map.put("invoiceNumber", payment.getInvoiceNumber());
        map.put("paidAt", payment.getPaidAt());
        return map;
    }

    private Map<String, Object> toReviewMap(Review review) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", review.getId());
        map.put("salonId", review.getSalon() != null ? review.getSalon().getId() : null);
        map.put("salonRating", review.getSalonRating());
        map.put("comment", review.getComment());
        map.put("status", review.getStatus());
        map.put("createdAt", review.getCreatedAt());
        return map;
    }

    private String writeExportFile(Long userId, Map<String, Object> payload) {
        try {
            Path targetDir = Paths.get(appProperties.getFile().getUploadDir(), EXPORT_SUB_DIRECTORY).normalize().toAbsolutePath();
            Files.createDirectories(targetDir);

            String fileName = "export-" + userId + "-" + RandomCodeGenerator.generateAlphanumeric(12) + ".json";
            Path targetPath = targetDir.resolve(fileName);

            Files.write(targetPath, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(payload));

            return "%s/%s/%s".formatted(appProperties.getFile().getBaseUrl(), EXPORT_SUB_DIRECTORY, fileName);
        } catch (IOException ex) {
            throw new FileStorageException("Failed to write GDPR/DPDP export file", ex);
        }
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    private DataSubjectRequest findRequest(Long requestId) {
        return dataSubjectRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("DataSubjectRequest", "id", requestId));
    }

    private DataSubjectRequestResponse toResponse(DataSubjectRequest dataSubjectRequest) {
        return DataSubjectRequestResponse.builder()
                .id(dataSubjectRequest.getId())
                .userId(dataSubjectRequest.getUser() != null ? dataSubjectRequest.getUser().getId() : null)
                .requestType(dataSubjectRequest.getRequestType())
                .status(dataSubjectRequest.getStatus())
                .requestedAt(dataSubjectRequest.getRequestedAt())
                .completedAt(dataSubjectRequest.getCompletedAt())
                .exportFileUrl(dataSubjectRequest.getExportFileUrl())
                .rejectionReason(dataSubjectRequest.getRejectionReason())
                .build();
    }
}
