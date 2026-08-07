package com.salofresh.service.crm;

import com.salofresh.dto.crm.CustomerNoteRequest;
import com.salofresh.dto.crm.CustomerNoteResponse;
import com.salofresh.dto.crm.CustomerProfileSummaryResponse;
import com.salofresh.response.PagedResponse;
import org.springframework.data.domain.Pageable;

/**
 * Salon-staff-facing CRM notes on a customer. Every method takes the id of
 * the currently authenticated user so ownership (and, for edits, authorship)
 * can be enforced against the salon/note being touched.
 */
public interface CustomerNoteService {

    CustomerNoteResponse create(Long requesterUserId, Long salonId, Long customerId, CustomerNoteRequest request);

    CustomerNoteResponse update(Long requesterUserId, Long salonId, Long customerId, Long noteId, CustomerNoteRequest request);

    void delete(Long requesterUserId, Long salonId, Long customerId, Long noteId);

    PagedResponse<CustomerNoteResponse> listForCustomer(Long requesterUserId, Long salonId, Long customerId, Pageable pageable);

    CustomerProfileSummaryResponse getCustomerProfileSummary(Long requesterUserId, Long salonId, Long customerId);
}
