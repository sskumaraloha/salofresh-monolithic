package com.salofresh.mapper.booking;

import com.salofresh.dto.booking.AppointmentResponse;
import com.salofresh.dto.booking.AppointmentSummaryResponse;
import com.salofresh.entity.Appointment;
import com.salofresh.entity.Employee;
import com.salofresh.entity.Salon;
import com.salofresh.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AppointmentMapper {

    /**
     * Maps the core appointment fields. The "services" line items are populated separately by the
     * caller (via AppointmentServiceMapper) since they are stored/fetched independently of the
     * Appointment entity's own relations.
     */
    @Mapping(target = "services", ignore = true)
    AppointmentResponse toResponse(Appointment appointment);

    AppointmentResponse.SalonSummary toSalonSummary(Salon salon);

    AppointmentResponse.EmployeeSummary toEmployeeSummary(Employee employee);

    AppointmentResponse.CustomerSummary toCustomerSummary(User user);

    @Mapping(target = "salonName", source = "salon.name")
    @Mapping(target = "employeeName", ignore = true)
    @Mapping(target = "serviceNames", ignore = true)
    AppointmentSummaryResponse toSummary(Appointment appointment);
}
