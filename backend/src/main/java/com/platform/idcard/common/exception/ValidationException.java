package com.platform.idcard.common.exception;

import org.springframework.http.HttpStatus;

public class ValidationException extends ApiException {
    public ValidationException(String message) {
        super(ErrorCode.VALIDATION_FAILED, message, HttpStatus.BAD_REQUEST);
    }
}
