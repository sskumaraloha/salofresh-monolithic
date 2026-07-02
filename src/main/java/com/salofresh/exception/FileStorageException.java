package com.salofresh.exception;

import org.springframework.http.HttpStatus;

public class FileStorageException extends BaseException {

    public FileStorageException(String message) {
        super(message, HttpStatus.BAD_REQUEST, ErrorCode.FILE_UPLOAD_ERROR);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, HttpStatus.BAD_REQUEST, ErrorCode.FILE_UPLOAD_ERROR);
        initCause(cause);
    }
}
