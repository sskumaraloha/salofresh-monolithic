package com.salofresh.otp;

import com.salofresh.common.enums.OtpChannel;
import com.salofresh.common.enums.OtpType;

public interface OtpService {

    void generateAndSend(String identifier, String recipientName, OtpType otpType, OtpChannel otpChannel);

    void resend(String identifier, OtpType otpType);

    void verify(String identifier, OtpType otpType, String code);

    boolean isVerified(String identifier, OtpType otpType);
}
