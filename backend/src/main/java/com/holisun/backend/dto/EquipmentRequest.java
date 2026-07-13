package com.holisun.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class EquipmentRequest {

    @NotBlank
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}