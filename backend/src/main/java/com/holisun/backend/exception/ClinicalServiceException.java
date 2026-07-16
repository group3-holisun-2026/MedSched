package com.holisun.backend.exception;

public class ClinicalServiceException extends RuntimeException {
    public ClinicalServiceException(String message) {
        super(message);

    }
    public ClinicalServiceException(Throwable e){
        super(e);
    }
}
