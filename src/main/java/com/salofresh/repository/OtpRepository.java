package com.salofresh.repository;

import com.salofresh.common.enums.OtpType;
import com.salofresh.entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpRepository extends JpaRepository<Otp, Long> {

    Optional<Otp> findTopByIdentifierAndOtpTypeAndVerifiedFalseOrderByCreatedAtDesc(String identifier, OtpType otpType);

    Optional<Otp> findTopByIdentifierAndOtpTypeOrderByCreatedAtDesc(String identifier, OtpType otpType);
}
