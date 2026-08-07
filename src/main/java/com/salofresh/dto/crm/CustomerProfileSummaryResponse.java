package com.salofresh.dto.crm;

import com.salofresh.common.enums.CustomerNoteTag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * A "customer 360" convenience view for salon staff: identity, spend/visit
 * aggregates at this salon, the note history, and any tags worth flagging
 * (e.g. VIP, allergy alerts) rolled up from those notes.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerProfileSummaryResponse {

    private CustomerSummaryResponse customer;
    private Long salonId;
    private long totalAppointments;
    private BigDecimal totalSpent;
    private boolean vip;
    private Set<CustomerNoteTag> aggregatedTags;
    private List<CustomerNoteResponse> notes;
}
