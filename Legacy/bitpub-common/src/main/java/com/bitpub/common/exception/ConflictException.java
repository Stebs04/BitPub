package com.bitpub.common.exception;

public class ConflictException extends BaseBusinessException {
    public ConflictException(String message, Object... args) {
        super(ErrorCode.CONFLICT, message, args);
    }
}
