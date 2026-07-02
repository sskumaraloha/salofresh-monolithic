package com.salofresh.repository;

import com.salofresh.entity.AppointmentService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppointmentServiceRepository extends JpaRepository<AppointmentService, Long> {

    List<AppointmentService> findAllByAppointmentId(Long appointmentId);
}
