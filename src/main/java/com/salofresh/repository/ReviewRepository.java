package com.salofresh.repository;

import com.salofresh.common.enums.ReviewStatus;
import com.salofresh.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {

    Page<Review> findAllBySalonIdAndDeletedFalse(Long salonId, Pageable pageable);

    Page<Review> findAllByEmployeeIdAndDeletedFalse(Long employeeId, Pageable pageable);

    Page<Review> findAllByCustomerIdAndDeletedFalse(Long customerId, Pageable pageable);

    Optional<Review> findByAppointmentId(Long appointmentId);

    boolean existsByAppointmentId(Long appointmentId);

    @Query("select avg(r.salonRating) from Review r where r.salon.id = :salonId and r.status = :status and r.deleted = false")
    Double averageSalonRating(@Param("salonId") Long salonId, @Param("status") ReviewStatus status);

    @Query("select count(r) from Review r where r.salon.id = :salonId and r.status = :status and r.deleted = false")
    long countBySalonIdAndStatus(@Param("salonId") Long salonId, @Param("status") ReviewStatus status);

    @Query("select avg(r.employeeRating) from Review r where r.employee.id = :employeeId and r.employeeRating is not null "
            + "and r.status = :status and r.deleted = false")
    Double averageEmployeeRating(@Param("employeeId") Long employeeId, @Param("status") ReviewStatus status);

    @Query("select avg(r.cleanlinessRating) from Review r where r.salon.id = :salonId and r.cleanlinessRating is not null "
            + "and r.status = :status and r.deleted = false")
    Double averageCleanlinessRating(@Param("salonId") Long salonId, @Param("status") ReviewStatus status);

    @Query("select avg(r.serviceQualityRating) from Review r where r.salon.id = :salonId and r.serviceQualityRating is not null "
            + "and r.status = :status and r.deleted = false")
    Double averageServiceQualityRating(@Param("salonId") Long salonId, @Param("status") ReviewStatus status);

    @Query("select avg(r.valueForMoneyRating) from Review r where r.salon.id = :salonId and r.valueForMoneyRating is not null "
            + "and r.status = :status and r.deleted = false")
    Double averageValueForMoneyRating(@Param("salonId") Long salonId, @Param("status") ReviewStatus status);
}
