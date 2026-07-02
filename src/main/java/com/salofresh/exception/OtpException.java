package com.salofresh.exception;

import org.springframework.http.HttpStatus;

public class OtpException extends BaseException {

    public OtpException(String message, ErrorCode errorCode) {
        super(message, HttpStatus.BAD_REQUEST, errorCode);
    }
}
