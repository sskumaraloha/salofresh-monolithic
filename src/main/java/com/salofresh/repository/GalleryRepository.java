package com.salofresh.repository;

import com.salofresh.entity.Gallery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GalleryRepository extends JpaRepository<Gallery, Long> {

    List<Gallery> findAllBySalonIdOrderBySortOrderAsc(Long salonId);

    Optional<Gallery> findByIdAndSalonId(Long id, Long salonId);
}
