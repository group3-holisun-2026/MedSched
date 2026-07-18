package com.holisun.backend.exception;

public class PatientException extends RuntimeException {
    public PatientException(String message) {
        super(message);
    }
    public PatientException(Throwable e){super(e);}
}
