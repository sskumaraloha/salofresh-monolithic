package com.salofresh.service.impl.crm;

import com.salofresh.common.enums.BookingStatus;
import com.salofresh.common.enums.CustomerNoteTag;
import com.salofresh.dto.crm.CustomerNoteRequest;
import com.salofresh.dto.crm.CustomerNoteResponse;
import com.salofresh.dto.crm.CustomerProfileSummaryResponse;
import com.salofresh.entity.Appointment;
import com.salofresh.entity.CustomerNote;
import com.salofresh.entity.Salon;
import com.salofresh.entity.User;
import com.salofresh.exception.ForbiddenException;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.mapper.crm.CustomerNoteMapper;
import com.salofresh.repository.AppointmentRepository;
import com.salofresh.repository.CustomerNoteRepository;
import com.salofresh.repository.EmployeeRepository;
import com.salofresh.repository.SalonRepository;
import com.salofresh.repository.UserRepository;
import com.salofresh.response.PagedResponse;
import com.salofresh.service.crm.CustomerNoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Ownership model (documented simplification):
 * <ul>
 *     <li>Only the {@code SalonOwner} that owns the salon may create, update,
 *     or delete CRM notes on its customers.</li>
 *     <li>An update may additionally be performed by the note's original
 *     author (checked via {@code createdByUser}), which matters if note
 *     authorship is ever extended beyond owners.</li>
 *     <li>Read access ({@code listForCustomer}, {@code getCustomerProfileSummary})
 *     is also granted to any {@code Employee} linked (via {@code Employee.user})
 *     to the salon, resolved in-memory from {@link EmployeeRepository}. In this
 *     revision the REST controller itself is locked to {@code SALON_OWNER} via
 *     {@code @PreAuthorize}, so the employee-read branch is presently unreachable
 *     over HTTP; it is kept here so a future employee-facing read endpoint can
 *     reuse this service without any change.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class CustomerNoteServiceImpl implements CustomerNoteService {

    private final CustomerNoteRepository customerNoteRepository;
    private final SalonRepository salonRepository;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final EmployeeRepository employeeRepository;
    private final CustomerNoteMapper customerNoteMapper;

    @Override
    @Transactional
    public CustomerNoteResponse create(Long requesterUserId, Long salonId, Long customerId, CustomerNoteRequest request) {
        Salon salon = getSalonOrThrow(salonId);
        verifyOwner(salon, requesterUserId);
        User customer = getCustomerOrThrow(customerId);
        User createdBy = getUserOrThrow(requesterUserId);

        CustomerNote note = CustomerNote.builder()
                .salon(salon)
                .customer(customer)
                .createdByUser(createdBy)
                .note(request.getNote())
                .tag(request.getTag())
                .build();

        CustomerNote saved = customerNoteRepository.save(note);
        return customerNoteMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CustomerNoteResponse update(Long requesterUserId, Long salonId, Long customerId, Long noteId, CustomerNoteRequest request) {
        Salon salon = getSalonOrThrow(salonId);
        CustomerNote note = getNoteOrThrow(salonId, customerId, noteId);
        verifyEditable(salon, note, requesterUserId);

        note.setNote(request.getNote());
        note.setTag(request.getTag());

        CustomerNote saved = customerNoteRepository.save(note);
        return customerNoteMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long requesterUserId, Long salonId, Long customerId, Long noteId) {
        Salon salon = getSalonOrThrow(salonId);
        verifyOwner(salon, requesterUserId);
        CustomerNote note = getNoteOrThrow(salonId, customerId, noteId);

        note.setDeleted(true);
        customerNoteRepository.save(note);
    }

    @Override
    public PagedResponse<CustomerNoteResponse> listForCustomer(Long requesterUserId, Long salonId, Long customerId, Pageable pageable) {
        Salon salon = getSalonOrThrow(salonId);
        verifyReadAccess(salon, requesterUserId);
        getCustomerOrThrow(customerId);

        Page<CustomerNote> page = customerNoteRepository
                .findAllBySalonIdAndCustomerIdAndDeletedFalseOrderByCreatedAtDesc(salonId, customerId, pageable);
        List<CustomerNoteResponse> content = customerNoteMapper.toResponseList(page.getContent());
        return PagedResponse.from(page, content);
    }

    @Override
    public CustomerProfileSummaryResponse getCustomerProfileSummary(Long requesterUserId, Long salonId, Long customerId) {
        Salon salon = getSalonOrThrow(salonId);
        verifyReadAccess(salon, requesterUserId);
        User customer = getCustomerOrThrow(customerId);

        List<CustomerNote> notes = customerNoteRepository
                .findAllBySalonIdAndCustomerIdAndDeletedFalseOrderByCreatedAtDesc(salonId, customerId, Pageable.unpaged())
                .getContent();

        Set<CustomerNoteTag> aggregatedTags = notes.stream()
                .map(CustomerNote::getTag)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(CustomerNoteTag.class)));
        boolean vip = aggregatedTags.contains(CustomerNoteTag.VIP);

        List<Appointment> customerAppointments = appointmentRepository.findAllBySalonId(salonId, Pageable.unpaged())
                .getContent().stream()
                .filter(a -> a.getCustomer() != null && a.getCustomer().getId().equals(customerId))
                .toList();

        long totalAppointments = customerAppointments.size();
        BigDecimal totalSpent = customerAppointments.stream()
                .filter(a -> a.getStatus() == BookingStatus.COMPLETED)
                .map(Appointment::getFinalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CustomerProfileSummaryResponse.builder()
                .customer(customerNoteMapper.toCustomerSummary(customer))
                .salonId(salonId)
                .totalAppointments(totalAppointments)
                .totalSpent(totalSpent)
                .vip(vip)
                .aggregatedTags(aggregatedTags)
                .notes(customerNoteMapper.toResponseList(notes))
                .build();
    }

    private Salon getSalonOrThrow(Long salonId) {
        return salonRepository.findByIdAndDeletedFalse(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon", "id", salonId));
    }

    private User getCustomerOrThrow(Long customerId) {
        return userRepository.findById(customerId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", customerId));
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    private CustomerNote getNoteOrThrow(Long salonId, Long customerId, Long noteId) {
        CustomerNote note = customerNoteRepository.findByIdAndSalonIdAndDeletedFalse(noteId, salonId)
                .orElseThrow(() -> new ResourceNotFoundException("CustomerNote", "id", noteId));
        if (note.getCustomer() == null || !note.getCustomer().getId().equals(customerId)) {
            throw new ResourceNotFoundException("CustomerNote", "id", noteId);
        }
        return note;
    }

    private boolean isOwner(Salon salon, Long userId) {
        return salon.getOwner() != null && salon.getOwner().getUser() != null
                && salon.getOwner().getUser().getId().equals(userId);
    }

    private boolean isLinkedEmployee(Long salonId, Long userId) {
        return employeeRepository.findAllBySalonIdAndDeletedFalse(salonId).stream()
                .anyMatch(e -> e.getUser() != null && e.getUser().getId().equals(userId));
    }

    private void verifyOwner(Salon salon, Long requesterUserId) {
        if (!isOwner(salon, requesterUserId)) {
            throw new ForbiddenException("Only the salon owner can manage CRM notes for this salon's customers");
        }
    }

    private void verifyReadAccess(Salon salon, Long requesterUserId) {
        if (!isOwner(salon, requesterUserId) && !isLinkedEmployee(salon.getId(), requesterUserId)) {
            throw new ForbiddenException("You do not have permission to view CRM notes for this salon's customers");
        }
    }

    private void verifyEditable(Salon salon, CustomerNote note, Long requesterUserId) {
        boolean isAuthor = note.getCreatedByUser() != null && note.getCreatedByUser().getId().equals(requesterUserId);
        if (!isOwner(salon, requesterUserId) && !isAuthor) {
            throw new ForbiddenException("Only the note's author or the salon owner can edit this note");
        }
    }
}
