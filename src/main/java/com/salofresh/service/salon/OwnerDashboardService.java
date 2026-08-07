package com.salofresh.service.salon;

import com.salofresh.dto.salon.BranchComparisonResponse;
import com.salofresh.dto.salon.OwnerDashboardResponse;

public interface OwnerDashboardService {

    OwnerDashboardResponse getDashboard();

    /**
     * Per-branch breakdown for owners managing multiple salons, so they can compare
     * performance across locations at a glance instead of switching context per salon.
     */
    BranchComparisonResponse getBranchComparison();
}
