package com.salofresh.repository;

import com.salofresh.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmailIgnoreCaseAndDeletedFalse(String email);

    Optional<User> findByPhoneAndDeletedFalse(String phone);

    Optional<User> findByEmailIgnoreCaseOrPhoneAndDeletedFalse(String email, String phone);

    Optional<User> findByReferralCode(String referralCode);

    Optional<User> findByProviderIdAndAuthProvider(String providerId, com.salofresh.common.enums.AuthProvider authProvider);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);

    boolean existsByReferralCode(String referralCode);

    // Added for fraud/anomaly detection: fetch recently-registered users that were referred by
    // someone, so we can group them by referredByCode in memory when looking for referral abuse.
    List<User> findAllByReferredByCodeIsNotNullAndCreatedAtAfter(Instant after);
}
