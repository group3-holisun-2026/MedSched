package com.holisun.backend.exception;

public class SmsTransientException extends RuntimeException {
    public SmsTransientException(String message) {
        super(message);
    }
    
    public SmsTransientException(String message, Throwable cause) {
        super(message, cause);
    }
}
