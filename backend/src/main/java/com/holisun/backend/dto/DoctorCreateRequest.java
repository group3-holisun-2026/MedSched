package com.holisun.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.UUID;

public record DoctorCreateRequest(
        @NotNull UUID userId,
        @NotBlank String speciality,
        @NotNull @Positive Integer standardConsultationDurationMinutes,
        List<WorkingHoursDto> schedule
) {}