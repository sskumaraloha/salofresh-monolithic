package com.salofresh.exception;

import org.springframework.http.HttpStatus;

public class SlotUnavailableException extends BaseException {

    public SlotUnavailableException(String message) {
        super(message, HttpStatus.CONFLICT, ErrorCode.SLOT_UNAVAILABLE);
    }
}
