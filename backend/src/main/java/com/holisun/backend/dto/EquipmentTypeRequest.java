package com.holisun.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EquipmentTypeRequest {

    @NotBlank
    private String name;

    private String description;
}
