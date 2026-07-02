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
public class BookingReportResponse {

    private List<BookingDataPoint> data;
    private long totalBookings;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingDataPoint {
        private String period;
        private long totalBookings;
        private long completed;
        private long cancelled;
        private long noShow;
    }
}
