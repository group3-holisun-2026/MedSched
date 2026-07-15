package com.holisun.backend.service;

public class ClinicalServiceException extends RuntimeException {
    public ClinicalServiceException(String message) {
        super(message);

    }
    public ClinicalServiceException(Throwable e){
        super(e);
    }
}
