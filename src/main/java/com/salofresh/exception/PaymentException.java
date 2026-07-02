package com.salofresh.exception;

import org.springframework.http.HttpStatus;

public class PaymentException extends BaseException {

    public PaymentException(String message) {
        super(message, HttpStatus.PAYMENT_REQUIRED, ErrorCode.PAYMENT_FAILED);
    }
}
