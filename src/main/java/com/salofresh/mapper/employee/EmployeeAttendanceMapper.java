package com.salofresh.mapper.employee;

import com.salofresh.dto.employee.AttendanceResponse;
import com.salofresh.entity.EmployeeAttendance;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EmployeeAttendanceMapper {

    @Mapping(target = "employeeId", expression = "java(attendance.getEmployee() != null ? attendance.getEmployee().getId() : null)")
    AttendanceResponse toResponse(EmployeeAttendance attendance);
}
