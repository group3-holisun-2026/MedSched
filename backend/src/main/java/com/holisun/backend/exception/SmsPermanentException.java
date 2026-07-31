package com.holisun.backend.exception;

public class SmsPermanentException extends RuntimeException {
    public SmsPermanentException(String message) {
        super(message);
    }
    
    public SmsPermanentException(String message, Throwable cause) {
        super(message, cause);
    }
}
