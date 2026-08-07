package com.salofresh.mapper.payroll;

import com.salofresh.dto.payroll.PayrollRecordResponse;
import com.salofresh.entity.Employee;
import com.salofresh.entity.PayrollRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PayrollRecordMapper {

    @Mapping(target = "employee", source = "employee")
    @Mapping(target = "salonId", expression = "java(payrollRecord.getSalon() != null ? payrollRecord.getSalon().getId() : null)")
    PayrollRecordResponse toResponse(PayrollRecord payrollRecord);

    @Mapping(target = "fullName", expression = "java(employee.getFullName())")
    PayrollRecordResponse.EmployeeSummary toEmployeeSummary(Employee employee);
}
