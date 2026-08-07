package com.salofresh.mapper.payroll;

import com.salofresh.dto.payroll.CommissionRuleRequest;
import com.salofresh.dto.payroll.CommissionRuleResponse;
import com.salofresh.entity.CommissionRule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CommissionRuleMapper {

    @Mapping(target = "salonId", expression = "java(rule.getSalon() != null ? rule.getSalon().getId() : null)")
    @Mapping(target = "employeeId", expression = "java(rule.getEmployee() != null ? rule.getEmployee().getId() : null)")
    @Mapping(target = "employeeName", expression = "java(rule.getEmployee() != null ? rule.getEmployee().getFullName() : null)")
    @Mapping(target = "categoryId", expression = "java(rule.getCategory() != null ? rule.getCategory().getId() : null)")
    @Mapping(target = "categoryName", expression = "java(rule.getCategory() != null ? rule.getCategory().getName() : null)")
    CommissionRuleResponse toResponse(CommissionRule rule);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "salon", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "active", ignore = true)
    CommissionRule toEntity(CommissionRuleRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "salon", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "active", ignore = true)
    void updateEntityFromRequest(CommissionRuleRequest request, @MappingTarget CommissionRule rule);
}
