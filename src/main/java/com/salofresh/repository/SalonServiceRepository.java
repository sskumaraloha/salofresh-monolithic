package com.salofresh.repository;

import com.salofresh.common.enums.EntityStatus;
import com.salofresh.entity.SalonService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface SalonServiceRepository extends JpaRepository<SalonService, Long>, JpaSpecificationExecutor<SalonService> {

    List<SalonService> findAllBySalonIdAndDeletedFalse(Long salonId);

    Optional<SalonService> findByIdAndSalonIdAndDeletedFalse(Long id, Long salonId);

    List<SalonService> findAllByCategoryIdAndDeletedFalse(Long categoryId);

    List<SalonService> findAllBySalonIdAndStatusAndDeletedFalse(Long salonId, EntityStatus status);
}
