package com.salofresh.mapper.salon;

import com.salofresh.dto.salon.HolidayRequest;
import com.salofresh.dto.salon.HolidayResponse;
import com.salofresh.entity.Holiday;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface HolidayMapper {

    HolidayResponse toResponse(Holiday holiday);

    List<HolidayResponse> toResponseList(List<Holiday> holidays);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "salon", ignore = true)
    Holiday toEntity(HolidayRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "salon", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void updateEntityFromRequest(HolidayRequest request, @MappingTarget Holiday holiday);
}
