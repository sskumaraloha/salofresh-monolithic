package com.salofresh.repository;

import com.salofresh.common.enums.ServiceCategoryType;
import com.salofresh.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByActiveTrue();

    Optional<Category> findByType(ServiceCategoryType type);

    boolean existsByType(ServiceCategoryType type);
}
