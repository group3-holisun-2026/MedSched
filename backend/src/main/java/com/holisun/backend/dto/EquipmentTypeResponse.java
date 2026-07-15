package com.holisun.backend.dto;

import java.util.UUID;

public record EquipmentTypeResponse(
        UUID id,
        String name,
        String description
) {}
