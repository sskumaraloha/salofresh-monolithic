package com.salofresh.service.catalog;

import com.salofresh.dto.catalog.SalonServiceCreateRequest;
import com.salofresh.dto.catalog.SalonServiceResponse;
import com.salofresh.dto.catalog.SalonServiceUpdateRequest;
import com.salofresh.dto.catalog.ServiceSearchRequest;
import com.salofresh.response.PagedResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SalonServiceCatalogService {

    SalonServiceResponse create(Long salonId, SalonServiceCreateRequest request);

    SalonServiceResponse update(Long salonId, Long serviceId, SalonServiceUpdateRequest request);

    void delete(Long salonId, Long serviceId);

    SalonServiceResponse toggleStatus(Long salonId, Long serviceId);

    SalonServiceResponse getById(Long salonId, Long serviceId);

    /**
     * Lists services for a salon.
     *
     * @param salonId        the salon id
     * @param ownerView      when {@code true} returns all non-deleted services regardless of status
     *                       (for the owning salon owner's management view); when {@code false} only
     *                       ACTIVE, non-deleted services are returned (public storefront view).
     */
    List<SalonServiceResponse> listBySalon(Long salonId, boolean ownerView);

    PagedResponse<SalonServiceResponse> search(ServiceSearchRequest request);

    SalonServiceResponse uploadImage(Long salonId, Long serviceId, MultipartFile file);
}
