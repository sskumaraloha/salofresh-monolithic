package com.salofresh.event;

import com.salofresh.common.enums.OtpChannel;
import com.salofresh.common.enums.OtpType;

public record OtpGeneratedEvent(String identifier, String recipientName, String otpCode,
                                 OtpType otpType, OtpChannel otpChannel) {
}
