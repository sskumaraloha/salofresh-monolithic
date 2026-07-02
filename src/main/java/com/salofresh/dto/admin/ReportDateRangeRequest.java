package com.salofresh.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportDateRangeRequest {

    private LocalDate fromDate;
    private LocalDate toDate;

    @Builder.Default
    private ReportGroupBy groupBy = ReportGroupBy.MONTH;
}
