package com.holisun.backend.exception;

public class EmailTransientException extends RuntimeException {
    public EmailTransientException(String message) {
        super(message);
    }
    public EmailTransientException(String message, Throwable cause) {
        super(message, cause);
    }
}
