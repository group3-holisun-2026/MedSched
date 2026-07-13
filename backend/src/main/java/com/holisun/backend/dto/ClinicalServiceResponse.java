package com.holisun.backend.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class ClinicalServiceResponse {

    private UUID id;
    private String name;
    private BigDecimal price;
    private Integer defaultDurationMinutes;
    private List<EquipmentResponse> requiredEquipment;

    public ClinicalServiceResponse() {
    }

    public ClinicalServiceResponse(UUID id, String name, BigDecimal price,
                                   Integer defaultDurationMinutes,
                                   List<EquipmentResponse> requiredEquipment) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.defaultDurationMinutes = defaultDurationMinutes;
        this.requiredEquipment = requiredEquipment;
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

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getDefaultDurationMinutes() {
        return defaultDurationMinutes;
    }

    public void setDefaultDurationMinutes(Integer defaultDurationMinutes) {
        this.defaultDurationMinutes = defaultDurationMinutes;
    }

    public List<EquipmentResponse> getRequiredEquipment() {
        return requiredEquipment;
    }

    public void setRequiredEquipment(List<EquipmentResponse> requiredEquipment) {
        this.requiredEquipment = requiredEquipment;
    }
}