package com.salofresh.service.inventory;

import com.salofresh.dto.inventory.ServiceProductUsageRequest;
import com.salofresh.dto.inventory.ServiceProductUsageResponse;

import java.util.List;

public interface ServiceProductUsageService {

    ServiceProductUsageResponse link(Long salonId, Long serviceId, ServiceProductUsageRequest request);

    void unlink(Long salonId, Long serviceId, Long usageId);

    List<ServiceProductUsageResponse> listForService(Long salonId, Long serviceId);
}
