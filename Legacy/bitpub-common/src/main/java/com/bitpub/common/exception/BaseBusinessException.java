package com.bitpub.common.exception;

import lombok.Getter;

@Getter
public abstract class BaseBusinessException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Object[] args;

    public BaseBusinessException(ErrorCode errorCode, String message, Object... args) {
        super(message != null ? message : errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.args = args;
    }

    public BaseBusinessException(ErrorCode errorCode, String message, Throwable cause, Object... args) {
        super(message != null ? message : errorCode.getDefaultMessage(), cause);
        this.errorCode = errorCode;
        this.args = args;
    }
}
