package com.salofresh.mapper.payment;

import com.salofresh.dto.payment.PaymentResponse;
import com.salofresh.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "appointmentId", source = "appointment.id")
    PaymentResponse toResponse(Payment payment);
}
