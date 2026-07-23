package com.holisun.backend.exception;
public class ResourceConflictException extends RuntimeException {

    public enum ResourceType {
        DOCTOR,
        ROOM,
        EQUIPMENT
    }

    private final ResourceType conflictingResource;

    public ResourceConflictException(ResourceType conflictingResource, String message) {

        super(message);
        this.conflictingResource = conflictingResource;
    }

    public ResourceType getConflictingResource() {
        return conflictingResource;
    }
}