package com.salofresh.mapper.employee;

import com.salofresh.dto.employee.LeaveResponse;
import com.salofresh.entity.EmployeeLeave;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EmployeeLeaveMapper {

    @Mapping(target = "employeeId", expression = "java(leave.getEmployee() != null ? leave.getEmployee().getId() : null)")
    LeaveResponse toResponse(EmployeeLeave leave);
}
