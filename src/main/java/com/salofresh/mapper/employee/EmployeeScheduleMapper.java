package com.salofresh.mapper.employee;

import com.salofresh.dto.employee.ScheduleResponse;
import com.salofresh.entity.EmployeeSchedule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EmployeeScheduleMapper {

    @Mapping(target = "employeeId", expression = "java(schedule.getEmployee() != null ? schedule.getEmployee().getId() : null)")
    ScheduleResponse toResponse(EmployeeSchedule schedule);
}
