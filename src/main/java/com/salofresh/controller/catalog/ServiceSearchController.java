package com.salofresh.controller.catalog;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.catalog.SalonServiceResponse;
import com.salofresh.dto.catalog.ServiceSearchRequest;
import com.salofresh.response.ApiResponse;
import com.salofresh.response.PagedResponse;
import com.salofresh.service.catalog.SalonServiceCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/services")
@RequiredArgsConstructor
@Tag(name = "Service Search", description = "Cross-salon service search and discovery")
public class ServiceSearchController {

    private final SalonServiceCatalogService salonServiceCatalogService;

    @GetMapping("/search")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Search ACTIVE services across all salons", description = "Public discovery endpoint")
    public ResponseEntity<ApiResponse<PagedResponse<SalonServiceResponse>>> search(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Long salonId,
            @RequestParam(name = "page", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(name = "size", defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.DEFAULT_SORT_BY) String sortBy,
            @RequestParam(name = "sortDirection", defaultValue = AppConstants.DEFAULT_SORT_DIRECTION) String sortDirection) {

        ServiceSearchRequest request = ServiceSearchRequest.builder()
                .categoryId(categoryId)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .salonId(salonId)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        return ResponseEntity.ok(ApiResponse.success("Services fetched successfully",
                salonServiceCatalogService.search(request)));
    }
}
