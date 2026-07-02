package com.salofresh.mapper.catalog;

import com.salofresh.dto.catalog.SalonServiceResponse;
import com.salofresh.entity.SalonService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = CategoryMapper.class)
public interface SalonServiceMapper {

    @Mapping(target = "salonId", source = "salon.id")
    @Mapping(target = "category", source = "category")
    @Mapping(target = "effectivePrice", expression = "java(salonService.getEffectivePrice())")
    SalonServiceResponse toResponse(SalonService salonService);
}
