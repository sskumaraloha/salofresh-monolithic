package com.salofresh.repository;

import com.salofresh.common.enums.MembershipStatus;
import com.salofresh.entity.PlatformSubscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlatformSubscriptionRepository extends JpaRepository<PlatformSubscription, Long> {

    Optional<PlatformSubscription> findFirstBySalonOwnerIdAndStatusOrderByEndDateDesc(Long salonOwnerId, MembershipStatus status);

    Page<PlatformSubscription> findAllBySalonOwnerIdOrderByCreatedAtDesc(Long salonOwnerId, Pageable pageable);

    Optional<PlatformSubscription> findByPaymentId(Long paymentId);
}
