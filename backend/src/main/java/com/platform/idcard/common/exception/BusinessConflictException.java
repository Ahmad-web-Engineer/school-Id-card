package com.platform.idcard.common.exception;

import org.springframework.http.HttpStatus;

public class BusinessConflictException extends ApiException {
    public BusinessConflictException(String message) {
        super(ErrorCode.RESOURCE_CONFLICT, message, HttpStatus.CONFLICT);
    }

    public BusinessConflictException(ErrorCode errorCode, String message) {
        super(errorCode, message, HttpStatus.CONFLICT);
    }
}
