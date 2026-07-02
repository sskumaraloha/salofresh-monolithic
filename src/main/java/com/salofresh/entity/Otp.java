package com.salofresh.entity;

import com.salofresh.audit.Auditable;
import com.salofresh.common.enums.OtpChannel;
import com.salofresh.common.enums.OtpType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

@Entity
@Table(name = "otp")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(callSuper = true, exclude = "otpCode")
public class Otp extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String identifier;

    @Column(name = "otp_code", nullable = false, length = 255)
    private String otpCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "otp_type", nullable = false, length = 30)
    private OtpType otpType;

    @Enumerated(EnumType.STRING)
    @Column(name = "otp_channel", nullable = false, length = 20)
    private OtpChannel otpChannel;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    @Column(nullable = false)
    @Builder.Default
    private boolean verified = false;

    @Column(nullable = false)
    @Builder.Default
    private int attempts = 0;

    @Column(name = "last_sent_at")
    private Instant lastSentAt;

    public boolean isExpired() {
        return expiryDate.isBefore(Instant.now());
    }
}
