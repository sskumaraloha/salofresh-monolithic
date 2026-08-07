package com.salofresh.dto.crm;

import com.salofresh.common.enums.CustomerNoteTag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerNoteResponse {

    private Long id;
    private CustomerSummaryResponse customer;
    private String createdByName;
    private String note;
    private CustomerNoteTag tag;
    private Instant createdAt;
}
