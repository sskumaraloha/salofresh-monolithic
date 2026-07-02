package com.salofresh.repository;

import com.salofresh.entity.RecentlyViewedSalon;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecentlyViewedSalonRepository extends JpaRepository<RecentlyViewedSalon, Long> {

    List<RecentlyViewedSalon> findAllByUserIdOrderByViewedAtDesc(Long userId, Pageable pageable);

    Optional<RecentlyViewedSalon> findByUserIdAndSalonId(Long userId, Long salonId);
}
