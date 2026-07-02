package com.salofresh.repository;

import com.salofresh.entity.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    Page<WalletTransaction> findAllByWalletIdOrderByCreatedAtDesc(Long walletId, Pageable pageable);
}
