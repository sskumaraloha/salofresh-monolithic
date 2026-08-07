package com.salofresh.mapper.shiftswap;

import com.salofresh.dto.shiftswap.ShiftSwapResponse;
import com.salofresh.entity.Employee;
import com.salofresh.entity.ShiftSwapRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ShiftSwapMapper {

    @Mapping(target = "salonId", source = "salon.id")
    @Mapping(target = "requestingEmployee", source = "requestingEmployee")
    @Mapping(target = "targetEmployee", source = "targetEmployee")
    @Mapping(target = "respondedByName",
            expression = "java(request.getRespondedBy() != null ? request.getRespondedBy().getFullName() : null)")
    ShiftSwapResponse toResponse(ShiftSwapRequest request);

    ShiftSwapResponse.EmployeeSummary toEmployeeSummary(Employee employee);
}
