package com.salofresh.repository;

import com.salofresh.entity.SalonOwner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SalonOwnerRepository extends JpaRepository<SalonOwner, Long> {

    Optional<SalonOwner> findByUserId(Long userId);
}
