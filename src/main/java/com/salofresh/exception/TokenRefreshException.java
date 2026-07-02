package com.salofresh.exception;

import org.springframework.http.HttpStatus;

public class TokenRefreshException extends BaseException {

    public TokenRefreshException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, ErrorCode.TOKEN_INVALID);
    }
}
