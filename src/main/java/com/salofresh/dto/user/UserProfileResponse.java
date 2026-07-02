package com.salofresh.dto.user;

import com.salofresh.common.enums.Gender;
import com.salofresh.common.enums.MembershipLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private Long id;
    private String email;
    private String phone;
    private String firstName;
    private String lastName;
    private String profileImageUrl;
    private Gender gender;
    private LocalDate dateOfBirth;
    private boolean emailVerified;
    private boolean phoneVerified;
    private String referralCode;
    private MembershipLevel membershipLevel;
    private int totalBookings;
    private BigDecimal totalSpent;
    private BigDecimal walletBalance;
}
