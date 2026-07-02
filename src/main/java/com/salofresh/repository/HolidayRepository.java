package com.salofresh.repository;

import com.salofresh.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    List<Holiday> findAllBySalonId(Long salonId);

    Optional<Holiday> findBySalonIdAndHolidayDate(Long salonId, LocalDate holidayDate);

    boolean existsBySalonIdAndHolidayDate(Long salonId, LocalDate holidayDate);

    Optional<Holiday> findByIdAndSalonId(Long id, Long salonId);
}
