package com.bitpub.network;

/**
 * Custom exception to indicate an error occurred during HTTP response parsing or
 * when an unexpected HTTP status code was received.
 *
 * @author Stefano Bellan 20054330
 * @version 1.0
 */
public class HttpParsingException extends RuntimeException {

    /**
     * Constructs a new HttpParsingException with the specified detail message.
     *
     * @param message the detail message describing the error
     */
    public HttpParsingException(String message) {
        super(message);
    }

    /**
     * Constructs a new HttpParsingException with the specified detail message and cause.
     *
     * @param message the detail message describing the error
     * @param cause the underlying cause of the parsing failure
     */
    public HttpParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
