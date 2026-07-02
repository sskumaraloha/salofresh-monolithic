package com.salofresh.mapper.salon;

import com.salofresh.dto.salon.WorkingHoursRequest;
import com.salofresh.dto.salon.WorkingHoursResponse;
import com.salofresh.entity.WorkingHours;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface WorkingHoursMapper {

    WorkingHoursResponse toResponse(WorkingHours workingHours);

    List<WorkingHoursResponse> toResponseList(List<WorkingHours> workingHours);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "salon", ignore = true)
    WorkingHours toEntity(WorkingHoursRequest request);
}
