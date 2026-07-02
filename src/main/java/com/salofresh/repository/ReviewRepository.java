package com.salofresh.repository;

import com.salofresh.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {

    Page<Review> findAllBySalonIdAndDeletedFalse(Long salonId, Pageable pageable);

    Page<Review> findAllByEmployeeIdAndDeletedFalse(Long employeeId, Pageable pageable);

    Page<Review> findAllByCustomerIdAndDeletedFalse(Long customerId, Pageable pageable);

    Optional<Review> findByAppointmentId(Long appointmentId);

    boolean existsByAppointmentId(Long appointmentId);
}
