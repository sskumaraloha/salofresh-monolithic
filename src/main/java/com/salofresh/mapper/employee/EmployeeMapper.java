package com.salofresh.mapper.employee;

import com.salofresh.dto.employee.AssignedServiceSummary;
import com.salofresh.dto.employee.EmployeeCreateRequest;
import com.salofresh.dto.employee.EmployeeResponse;
import com.salofresh.dto.employee.EmployeeUpdateRequest;
import com.salofresh.entity.Employee;
import com.salofresh.entity.SalonService;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface EmployeeMapper {

    @Mapping(target = "salonId", expression = "java(employee.getSalon() != null ? employee.getSalon().getId() : null)")
    @Mapping(target = "fullName", expression = "java(employee.getFullName())")
    @Mapping(target = "assignedServices", source = "services")
    EmployeeResponse toResponse(Employee employee);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "salon", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "services", ignore = true)
    @Mapping(target = "employmentStatus", ignore = true)
    @Mapping(target = "profileImageUrl", ignore = true)
    @Mapping(target = "ratingAverage", ignore = true)
    Employee toEntity(EmployeeCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "salon", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "services", ignore = true)
    @Mapping(target = "employmentStatus", ignore = true)
    @Mapping(target = "profileImageUrl", ignore = true)
    @Mapping(target = "ratingAverage", ignore = true)
    void updateEntityFromRequest(EmployeeUpdateRequest request, @MappingTarget Employee employee);

    AssignedServiceSummary toServiceSummary(SalonService salonService);

    List<AssignedServiceSummary> toServiceSummaries(Set<SalonService> services);
}
