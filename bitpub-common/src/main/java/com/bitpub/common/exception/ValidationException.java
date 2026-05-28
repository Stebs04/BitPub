package com.bitpub.common.exception;

public class ValidationException extends BaseBusinessException {
    public ValidationException(String message, Object... args) {
        super(ErrorCode.VALIDATION_FAILED, message, args);
    }
}
