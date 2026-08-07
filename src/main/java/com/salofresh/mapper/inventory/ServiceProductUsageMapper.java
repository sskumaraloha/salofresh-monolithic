package com.salofresh.mapper.inventory;

import com.salofresh.dto.inventory.ServiceProductUsageResponse;
import com.salofresh.entity.ServiceProductUsage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ServiceProductUsageMapper {

    @Mapping(target = "serviceId", source = "service.id")
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    ServiceProductUsageResponse toResponse(ServiceProductUsage usage);

    List<ServiceProductUsageResponse> toResponseList(List<ServiceProductUsage> usages);
}
