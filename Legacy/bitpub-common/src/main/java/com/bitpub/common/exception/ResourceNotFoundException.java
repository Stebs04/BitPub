package com.bitpub.common.exception;

public class ResourceNotFoundException extends BaseBusinessException {
    public ResourceNotFoundException(String message, Object... args) {
        super(ErrorCode.NOT_FOUND, message, args);
    }
}
