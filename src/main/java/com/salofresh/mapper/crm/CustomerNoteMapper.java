package com.salofresh.mapper.crm;

import com.salofresh.dto.crm.CustomerNoteResponse;
import com.salofresh.dto.crm.CustomerSummaryResponse;
import com.salofresh.entity.CustomerNote;
import com.salofresh.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CustomerNoteMapper {

    @Mapping(target = "customer", source = "customer")
    @Mapping(target = "createdByName",
            expression = "java(note.getCreatedByUser() != null ? note.getCreatedByUser().getFullName() : null)")
    CustomerNoteResponse toResponse(CustomerNote note);

    List<CustomerNoteResponse> toResponseList(List<CustomerNote> notes);

    @Mapping(target = "fullName", expression = "java(customer.getFullName())")
    CustomerSummaryResponse toCustomerSummary(User customer);
}
