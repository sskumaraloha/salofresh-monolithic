package com.salofresh.repository;

import com.salofresh.entity.StockTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockTransactionRepository extends JpaRepository<StockTransaction, Long> {

    Page<StockTransaction> findAllByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);
}
