package com.holisun.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ClinicalServiceResponse {
    private UUID id;
    private String name;
    private BigDecimal price;
    private Integer defaultDurationMinutes;
    private Set<EquipmentTypeResponse> requiredEquipmentTypes;

    private Boolean active;
}
