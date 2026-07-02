package com.salofresh.mapper.salon;

import com.salofresh.dto.salon.SalonCreateRequest;
import com.salofresh.dto.salon.SalonResponse;
import com.salofresh.dto.salon.SalonSummaryResponse;
import com.salofresh.dto.salon.SalonUpdateRequest;
import com.salofresh.entity.Salon;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SalonMapper {

    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "ownerBusinessName", source = "owner.businessName")
    @Mapping(target = "cityId", source = "city.id")
    @Mapping(target = "cityName", source = "city.name")
    @Mapping(target = "stateName", source = "city.state.name")
    @Mapping(target = "countryName", source = "city.state.country.name")
    @Mapping(target = "galleryImages", ignore = true)
    @Mapping(target = "workingHours", ignore = true)
    @Mapping(target = "holidays", ignore = true)
    @Mapping(target = "distanceKm", ignore = true)
    SalonResponse toResponse(Salon salon);

    @Mapping(target = "city", source = "city.name")
    @Mapping(target = "startingPrice", ignore = true)
    @Mapping(target = "distanceKm", ignore = true)
    SalonSummaryResponse toSummary(Salon salon);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "city", ignore = true)
    @Mapping(target = "bannerImageUrl", ignore = true)
    @Mapping(target = "verificationStatus", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "ratingAverage", ignore = true)
    @Mapping(target = "reviewCount", ignore = true)
    @Mapping(target = "slotDurationMinutes", ignore = true)
    @Mapping(target = "bufferTimeMinutes", ignore = true)
    @Mapping(target = "maxBookingsPerSlot", ignore = true)
    Salon toEntity(SalonCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "city", ignore = true)
    @Mapping(target = "bannerImageUrl", ignore = true)
    @Mapping(target = "verificationStatus", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "ratingAverage", ignore = true)
    @Mapping(target = "reviewCount", ignore = true)
    @Mapping(target = "slotDurationMinutes", ignore = true)
    @Mapping(target = "bufferTimeMinutes", ignore = true)
    @Mapping(target = "maxBookingsPerSlot", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void updateEntityFromRequest(SalonUpdateRequest request, @MappingTarget Salon salon);
}
