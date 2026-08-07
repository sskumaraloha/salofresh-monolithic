package com.salofresh.service.impl.inventory;

import com.salofresh.dto.inventory.ServiceProductUsageRequest;
import com.salofresh.dto.inventory.ServiceProductUsageResponse;
import com.salofresh.entity.Product;
import com.salofresh.entity.SalonService;
import com.salofresh.entity.ServiceProductUsage;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.mapper.inventory.ServiceProductUsageMapper;
import com.salofresh.repository.ProductRepository;
import com.salofresh.repository.SalonServiceRepository;
import com.salofresh.repository.ServiceProductUsageRepository;
import com.salofresh.service.inventory.ServiceProductUsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceProductUsageServiceImpl implements ServiceProductUsageService {

    private final SalonServiceRepository salonServiceRepository;
    private final ProductRepository productRepository;
    private final ServiceProductUsageRepository serviceProductUsageRepository;
    private final ServiceProductUsageMapper serviceProductUsageMapper;
    private final InventoryAccessGuard inventoryAccessGuard;

    @Override
    @Transactional
    public ServiceProductUsageResponse link(Long salonId, Long serviceId, ServiceProductUsageRequest request) {
        inventoryAccessGuard.requireOwnedSalon(salonId);
        SalonService service = findService(salonId, serviceId);
        Product product = productRepository.findByIdAndSalonIdAndDeletedFalse(request.getProductId(), salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

        ServiceProductUsage usage = ServiceProductUsage.builder()
                .service(service)
                .product(product)
                .quantityPerService(request.getQuantityPerService())
                .build();
        return serviceProductUsageMapper.toResponse(serviceProductUsageRepository.save(usage));
    }

    @Override
    @Transactional
    public void unlink(Long salonId, Long serviceId, Long usageId) {
        inventoryAccessGuard.requireOwnedSalon(salonId);
        findService(salonId, serviceId);
        ServiceProductUsage usage = serviceProductUsageRepository.findById(usageId)
                .orElseThrow(() -> new ResourceNotFoundException("Service product usage", "id", usageId));
        if (usage.getService() == null || !usage.getService().getId().equals(serviceId)) {
            throw new ResourceNotFoundException("Service product usage", "id", usageId);
        }
        serviceProductUsageRepository.delete(usage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceProductUsageResponse> listForService(Long salonId, Long serviceId) {
        inventoryAccessGuard.requireOwnedSalon(salonId);
        findService(salonId, serviceId);
        return serviceProductUsageMapper.toResponseList(serviceProductUsageRepository.findAllByServiceId(serviceId));
    }

    private SalonService findService(Long salonId, Long serviceId) {
        return salonServiceRepository.findByIdAndSalonIdAndDeletedFalse(serviceId, salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Service", "id", serviceId));
    }
}
