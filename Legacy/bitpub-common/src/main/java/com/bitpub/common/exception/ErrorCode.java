package com.bitpub.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    INTERNAL_SERVER_ERROR("ERR-500", "Internal Server Error"),
    BAD_REQUEST("ERR-400", "Bad Request"),
    UNAUTHORIZED("ERR-401", "Unauthorized"),
    FORBIDDEN("ERR-403", "Forbidden"),
    NOT_FOUND("ERR-404", "Resource Not Found"),
    CONFLICT("ERR-409", "Conflict"),
    VALIDATION_FAILED("ERR-422", "Validation Failed"),
    MQTT_ERROR("ERR-503", "MQTT Processing Error");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }
}
