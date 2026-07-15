package com.holisun.backend.dto;

import java.util.UUID;

public class EquipmentResponse {

    private UUID id;
    private String name;
    private boolean active;

    public EquipmentResponse() {
    }

    public EquipmentResponse(UUID id, String name, boolean active) {
        this.id = id;
        this.name = name;
        this.active = active;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}