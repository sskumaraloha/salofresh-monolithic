package com.salofresh.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerReportResponse {

    private List<CustomerDataPoint> data;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerDataPoint {
        private String period;
        private long newCustomers;
        private long activeCustomers;
    }
}
