package com.salofresh.repository;

import com.salofresh.entity.GiftCard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GiftCardRepository extends JpaRepository<GiftCard, Long> {

    Optional<GiftCard> findByCodeIgnoreCase(String code);

    Page<GiftCard> findAllByPurchasedByIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    boolean existsByCodeIgnoreCase(String code);
}
