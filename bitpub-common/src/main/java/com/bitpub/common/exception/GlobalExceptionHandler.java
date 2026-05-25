package com.bitpub.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(BaseBusinessException.class)
    public ResponseEntity<ApiError> handleBusinessException(BaseBusinessException ex, HttpServletRequest request) {
        String message = resolveMessage(ex.getMessage(), ex.getArgs());
        HttpStatus status = resolveStatus(ex.getErrorCode());

        ApiError apiError = ApiError.builder()
                .status(status.value())
                .title(ex.getErrorCode().getDefaultMessage())
                .code(ex.getErrorCode().getCode())
                .message(message)
                .path(request.getRequestURI())
                .traceId(getTraceId())
                .build();

        return ResponseEntity.status(status).body(apiError);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, Object> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ApiError apiError = ApiError.builder()
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .title(ErrorCode.VALIDATION_FAILED.getDefaultMessage())
                .code(ErrorCode.VALIDATION_FAILED.getCode())
                .message("Validation error")
                .details(errors)
                .path(request.getRequestURI())
                .traceId(getTraceId())
                .build();

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(apiError);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, Object> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            errors.put(violation.getPropertyPath().toString(), violation.getMessage());
        });

        ApiError apiError = ApiError.builder()
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .title(ErrorCode.VALIDATION_FAILED.getDefaultMessage())
                .code(ErrorCode.VALIDATION_FAILED.getCode())
                .message("Constraint violation")
                .details(errors)
                .path(request.getRequestURI())
                .traceId(getTraceId())
                .build();

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(apiError);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        ApiError apiError = ApiError.builder()
                .status(HttpStatus.FORBIDDEN.value())
                .title(ErrorCode.FORBIDDEN.getDefaultMessage())
                .code(ErrorCode.FORBIDDEN.getCode())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .traceId(getTraceId())
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(apiError);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        ApiError apiError = ApiError.builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .title(ErrorCode.UNAUTHORIZED.getDefaultMessage())
                .code(ErrorCode.UNAUTHORIZED.getCode())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .traceId(getTraceId())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(apiError);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAllUncaughtException(Exception ex, HttpServletRequest request) {
        ApiError apiError = ApiError.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .title(ErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage())
                .code(ErrorCode.INTERNAL_SERVER_ERROR.getCode())
                .message("An unexpected error occurred")
                .path(request.getRequestURI())
                .traceId(getTraceId())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiError);
    }

    private String resolveMessage(String messageKey, Object[] args) {
        try {
            return messageSource.getMessage(messageKey, args, LocaleContextHolder.getLocale());
        } catch (Exception e) {
            return messageKey;
        }
    }

    private HttpStatus resolveStatus(ErrorCode errorCode) {
        switch (errorCode) {
            case BAD_REQUEST: return HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED: return HttpStatus.UNAUTHORIZED;
            case FORBIDDEN: return HttpStatus.FORBIDDEN;
            case NOT_FOUND: return HttpStatus.NOT_FOUND;
            case CONFLICT: return HttpStatus.CONFLICT;
            case VALIDATION_FAILED: return HttpStatus.UNPROCESSABLE_ENTITY;
            case MQTT_ERROR: return HttpStatus.SERVICE_UNAVAILABLE;
            default: return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

    private String getTraceId() {
        return MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
    }
}
