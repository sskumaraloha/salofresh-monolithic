package com.salofresh.repository;

import com.salofresh.entity.Salon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface SalonRepository extends JpaRepository<Salon, Long>, JpaSpecificationExecutor<Salon> {

    Optional<Salon> findBySlugAndDeletedFalse(String slug);

    List<Salon> findAllByOwnerIdAndDeletedFalse(Long ownerId);

    boolean existsBySlug(String slug);

    long countByOwnerIdAndDeletedFalse(Long ownerId);

    Optional<Salon> findByIdAndDeletedFalse(Long id);
}
