package com.salofresh.repository;

import com.salofresh.common.enums.MembershipStatus;
import com.salofresh.entity.MembershipSubscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembershipSubscriptionRepository extends JpaRepository<MembershipSubscription, Long> {

    Page<MembershipSubscription> findAllByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<MembershipSubscription> findAllByUserIdAndPlan_Salon_IdAndStatus(Long userId, Long salonId, MembershipStatus status);

    Optional<MembershipSubscription> findByIdAndUserId(Long id, Long userId);

    /**
     * Looks up the (pre-created, payment-linked) subscription for a given purchase payment so that
     * a later gateway/cash confirmation can finalize or cancel it without needing to re-resolve the
     * originally purchased plan.
     */
    Optional<MembershipSubscription> findByPaymentId(Long paymentId);
}
