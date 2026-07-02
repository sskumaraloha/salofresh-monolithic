package com.salofresh.service.impl.catalog;

import com.salofresh.common.enums.EntityStatus;
import com.salofresh.dto.catalog.SalonServiceCreateRequest;
import com.salofresh.dto.catalog.SalonServiceResponse;
import com.salofresh.dto.catalog.SalonServiceUpdateRequest;
import com.salofresh.dto.catalog.ServiceSearchRequest;
import com.salofresh.entity.Category;
import com.salofresh.entity.Salon;
import com.salofresh.entity.SalonService;
import com.salofresh.exception.ForbiddenException;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.file.FileStorageService;
import com.salofresh.mapper.catalog.SalonServiceMapper;
import com.salofresh.repository.CategoryRepository;
import com.salofresh.repository.SalonRepository;
import com.salofresh.repository.SalonServiceRepository;
import com.salofresh.response.PagedResponse;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.catalog.SalonServiceCatalogService;
import com.salofresh.specification.SalonServiceSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SalonServiceCatalogServiceImpl implements SalonServiceCatalogService {

    private static final String IMAGE_SUB_DIRECTORY = "service-images";

    private final SalonServiceRepository salonServiceRepository;
    private final SalonRepository salonRepository;
    private final CategoryRepository categoryRepository;
    private final SalonServiceMapper salonServiceMapper;
    private final SecurityUtils securityUtils;
    private final FileStorageService fileStorageService;

    @Override
    public SalonServiceResponse create(Long salonId, SalonServiceCreateRequest request) {
        Salon salon = getSalonOrThrow(salonId);
        verifyOwnership(salon);
        Category category = getCategoryOrThrow(request.getCategoryId());

        SalonService service = SalonService.builder()
                .salon(salon)
                .category(category)
                .name(request.getName())
                .description(request.getDescription())
                .durationMinutes(request.getDurationMinutes())
                .price(request.getPrice())
                .discountPrice(request.getDiscountPrice())
                .taxPercentage(request.getTaxPercentage())
                .status(request.getStatus() != null ? request.getStatus() : EntityStatus.ACTIVE)
                .imageUrl(request.getImageUrl())
                .build();

        return salonServiceMapper.toResponse(salonServiceRepository.save(service));
    }

    @Override
    public SalonServiceResponse update(Long salonId, Long serviceId, SalonServiceUpdateRequest request) {
        SalonService service = getServiceOrThrow(salonId, serviceId);
        verifyOwnership(service.getSalon());

        if (!service.getCategory().getId().equals(request.getCategoryId())) {
            service.setCategory(getCategoryOrThrow(request.getCategoryId()));
        }
        service.setName(request.getName());
        service.setDescription(request.getDescription());
        service.setDurationMinutes(request.getDurationMinutes());
        service.setPrice(request.getPrice());
        service.setDiscountPrice(request.getDiscountPrice());
        service.setTaxPercentage(request.getTaxPercentage());
        if (request.getStatus() != null) {
            service.setStatus(request.getStatus());
        }
        if (request.getImageUrl() != null) {
            service.setImageUrl(request.getImageUrl());
        }

        return salonServiceMapper.toResponse(salonServiceRepository.save(service));
    }

    @Override
    public void delete(Long salonId, Long serviceId) {
        SalonService service = getServiceOrThrow(salonId, serviceId);
        verifyOwnership(service.getSalon());
        service.setDeleted(true);
        salonServiceRepository.save(service);
    }

    @Override
    public SalonServiceResponse toggleStatus(Long salonId, Long serviceId) {
        SalonService service = getServiceOrThrow(salonId, serviceId);
        verifyOwnership(service.getSalon());
        service.setStatus(service.getStatus() == EntityStatus.ACTIVE ? EntityStatus.INACTIVE : EntityStatus.ACTIVE);
        return salonServiceMapper.toResponse(salonServiceRepository.save(service));
    }

    @Override
    @Transactional(readOnly = true)
    public SalonServiceResponse getById(Long salonId, Long serviceId) {
        return salonServiceMapper.toResponse(getServiceOrThrow(salonId, serviceId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalonServiceResponse> listBySalon(Long salonId, boolean ownerView) {
        List<SalonService> services;
        if (ownerView) {
            Salon salon = getSalonOrThrow(salonId);
            verifyOwnership(salon);
            services = salonServiceRepository.findAllBySalonIdAndDeletedFalse(salonId);
        } else {
            services = salonServiceRepository.findAllBySalonIdAndStatusAndDeletedFalse(salonId, EntityStatus.ACTIVE);
        }
        return services.stream().map(salonServiceMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<SalonServiceResponse> search(ServiceSearchRequest request) {
        Specification<SalonService> spec = Specification
                .where(SalonServiceSpecification.notDeleted())
                .and(SalonServiceSpecification.isActive())
                .and(SalonServiceSpecification.hasSalon(request.getSalonId()))
                .and(SalonServiceSpecification.hasCategory(request.getCategoryId()))
                .and(SalonServiceSpecification.priceBetween(request.getMinPrice(), request.getMaxPrice()));

        Sort.Direction direction = "ASC".equalsIgnoreCase(request.getSortDirection()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), Sort.by(direction, request.getSortBy()));

        var page = salonServiceRepository.findAll(spec, pageable);
        List<SalonServiceResponse> content = page.getContent().stream().map(salonServiceMapper::toResponse).toList();
        return PagedResponse.from(page, content);
    }

    @Override
    public SalonServiceResponse uploadImage(Long salonId, Long serviceId, MultipartFile file) {
        SalonService service = getServiceOrThrow(salonId, serviceId);
        verifyOwnership(service.getSalon());
        String imageUrl = fileStorageService.store(file, IMAGE_SUB_DIRECTORY);
        service.setImageUrl(imageUrl);
        return salonServiceMapper.toResponse(salonServiceRepository.save(service));
    }

    private Salon getSalonOrThrow(Long salonId) {
        return salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon", "id", salonId));
    }

    private Category getCategoryOrThrow(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));
    }

    private SalonService getServiceOrThrow(Long salonId, Long serviceId) {
        return salonServiceRepository.findByIdAndSalonIdAndDeletedFalse(serviceId, salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Service", "id", serviceId));
    }

    private void verifyOwnership(Salon salon) {
        Long currentUserId = securityUtils.getCurrentUserId();
        if (salon.getOwner() == null || salon.getOwner().getUser() == null
                || !salon.getOwner().getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException("You do not have permission to manage services for this salon");
        }
    }
}
