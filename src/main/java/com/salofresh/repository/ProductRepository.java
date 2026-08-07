package com.salofresh.repository;

import com.salofresh.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findAllBySalonIdAndDeletedFalse(Long salonId);

    Optional<Product> findByIdAndSalonIdAndDeletedFalse(Long id, Long salonId);
}
