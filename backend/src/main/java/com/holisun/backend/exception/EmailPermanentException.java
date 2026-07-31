package com.holisun.backend.exception;

public class EmailPermanentException extends RuntimeException {
    public EmailPermanentException(String message) {
        super(message);
    }
    public EmailPermanentException(String message, Throwable cause) {
        super(message, cause);
    }
}
