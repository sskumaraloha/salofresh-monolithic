package com.salofresh.otp;

import com.salofresh.common.enums.OtpChannel;
import com.salofresh.common.enums.OtpType;
import com.salofresh.config.AppProperties;
import com.salofresh.entity.Otp;
import com.salofresh.event.OtpGeneratedEvent;
import com.salofresh.exception.BadRequestException;
import com.salofresh.exception.ErrorCode;
import com.salofresh.exception.OtpException;
import com.salofresh.repository.OtpRepository;
import com.salofresh.util.RandomCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private final OtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void generateAndSend(String identifier, String recipientName, OtpType otpType, OtpChannel otpChannel) {
        String code = RandomCodeGenerator.generateNumericOtp(appProperties.getOtp().getLength());

        Otp otp = Otp.builder()
                .identifier(identifier)
                .otpCode(passwordEncoder.encode(code))
                .otpType(otpType)
                .otpChannel(otpChannel)
                .expiryDate(Instant.now().plus(Duration.ofMinutes(appProperties.getOtp().getExpiryMinutes())))
                .verified(false)
                .attempts(0)
                .lastSentAt(Instant.now())
                .build();
        otpRepository.save(otp);

        eventPublisher.publishEvent(new OtpGeneratedEvent(identifier, recipientName, code, otpType, otpChannel));
    }

    @Override
    @Transactional
    public void resend(String identifier, OtpType otpType) {
        Otp lastOtp = otpRepository.findTopByIdentifierAndOtpTypeAndVerifiedFalseOrderByCreatedAtDesc(identifier, otpType)
                .orElseThrow(() -> new BadRequestException("No pending OTP request found for this identifier"));

        if (lastOtp.getLastSentAt() != null) {
            long secondsSinceLastSend = Duration.between(lastOtp.getLastSentAt(), Instant.now()).toSeconds();
            if (secondsSinceLastSend < appProperties.getOtp().getResendCooldownSeconds()) {
                throw new BadRequestException("Please wait before requesting another OTP");
            }
        }

        generateAndSend(identifier, identifier, otpType, lastOtp.getOtpChannel());
    }

    @Override
    @Transactional
    public void verify(String identifier, OtpType otpType, String code) {
        Otp otp = otpRepository.findTopByIdentifierAndOtpTypeAndVerifiedFalseOrderByCreatedAtDesc(identifier, otpType)
                .orElseThrow(() -> new OtpException("No OTP request found for this identifier", ErrorCode.OTP_INVALID));

        if (otp.isExpired()) {
            throw new OtpException("OTP has expired, please request a new one", ErrorCode.OTP_EXPIRED);
        }
        if (otp.getAttempts() >= appProperties.getOtp().getMaxAttempts()) {
            throw new OtpException("Maximum verification attempts exceeded, please request a new OTP", ErrorCode.OTP_INVALID);
        }

        otp.setAttempts(otp.getAttempts() + 1);
        if (!passwordEncoder.matches(code, otp.getOtpCode())) {
            otpRepository.save(otp);
            throw new OtpException("Invalid OTP code", ErrorCode.OTP_INVALID);
        }

        otp.setVerified(true);
        otpRepository.save(otp);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isVerified(String identifier, OtpType otpType) {
        return otpRepository.findTopByIdentifierAndOtpTypeOrderByCreatedAtDesc(identifier, otpType)
                .map(Otp::isVerified)
                .orElse(false);
    }
}
