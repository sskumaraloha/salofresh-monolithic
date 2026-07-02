package com.salofresh.mapper.booking;

import com.salofresh.dto.booking.AppointmentServiceLineItemResponse;
import com.salofresh.entity.AppointmentService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AppointmentServiceMapper {

    @Mapping(target = "serviceId", source = "service.id")
    AppointmentServiceLineItemResponse toResponse(AppointmentService appointmentService);

    List<AppointmentServiceLineItemResponse> toResponseList(List<AppointmentService> appointmentServices);
}
