package it.uniupo.pissir.bitpub.common.exception;

import it.uniupo.pissir.bitpub.common.dto.ErrorResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;

@ControllerAdvice
@ConditionalOnClass(name = "org.springframework.security.access.AccessDeniedException")
public class SecurityExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .message("Access denied: insufficient role privileges")
                .path("Unknown")
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }
}
