package com.salofresh.repository;

import com.salofresh.entity.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    Page<Favorite> findAllByUserId(Long userId, Pageable pageable);

    Optional<Favorite> findByUserIdAndSalonId(Long userId, Long salonId);

    boolean existsByUserIdAndSalonId(Long userId, Long salonId);

    void deleteByUserIdAndSalonId(Long userId, Long salonId);
}
