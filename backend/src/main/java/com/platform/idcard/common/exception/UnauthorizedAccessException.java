package com.platform.idcard.common.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedAccessException extends ApiException {
    public UnauthorizedAccessException(String message) {
        super(ErrorCode.RECORD_ACCESS_FORBIDDEN, message, HttpStatus.FORBIDDEN);
    }

    public UnauthorizedAccessException(ErrorCode errorCode, String message) {
        super(errorCode, message, HttpStatus.FORBIDDEN);
    }
}
