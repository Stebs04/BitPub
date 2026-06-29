package com.bitpub.common.exception;

public class MqttProcessingException extends BaseBusinessException {
    public MqttProcessingException(String message, Throwable cause, Object... args) {
        super(ErrorCode.MQTT_ERROR, message, cause, args);
    }

    public MqttProcessingException(String message, Object... args) {
        super(ErrorCode.MQTT_ERROR, message, args);
    }
}
