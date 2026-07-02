package com.salofresh.repository;

import com.salofresh.entity.WorkingHours;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

public interface WorkingHoursRepository extends JpaRepository<WorkingHours, Long> {

    List<WorkingHours> findAllBySalonId(Long salonId);

    Optional<WorkingHours> findBySalonIdAndDayOfWeek(Long salonId, DayOfWeek dayOfWeek);

    void deleteAllBySalonId(Long salonId);
}
