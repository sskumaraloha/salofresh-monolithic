package com.salofresh.mapper.waitlist;

import com.salofresh.dto.waitlist.WaitlistResponse;
import com.salofresh.entity.Waitlist;
import com.salofresh.mapper.salon.SalonMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = SalonMapper.class)
public interface WaitlistMapper {

    @Mapping(target = "salon", source = "salon")
    @Mapping(target = "employeeName",
            expression = "java(waitlist.getEmployee() != null ? waitlist.getEmployee().getFullName() : null)")
    @Mapping(target = "serviceName",
            expression = "java(waitlist.getService() != null ? waitlist.getService().getName() : null)")
    WaitlistResponse toResponse(Waitlist waitlist);
}
