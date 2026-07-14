package com.holisun.backend.dto;


import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ClinicalServiceRequest {
    @NotBlank
    @Size(max = 50) // longest procedure(service) name: "pharyngolaryngoesophagectomy"
    private String name;

    @Positive
    private BigDecimal price;

    @Positive
    private Integer defaultDurationMinutes;

    @NotNull
    private List<UUID> requiredEquipmentTypeIds;

    private Boolean active; //unsure about this
}
