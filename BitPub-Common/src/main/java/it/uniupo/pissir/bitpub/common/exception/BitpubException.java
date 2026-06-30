package it.uniupo.pissir.bitpub.common.exception;

import org.springframework.http.HttpStatus;

public class BitpubException extends RuntimeException {
    
    private final HttpStatus status;
    
    public BitpubException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
    
    public HttpStatus getStatus() {
        return status;
    }
}
