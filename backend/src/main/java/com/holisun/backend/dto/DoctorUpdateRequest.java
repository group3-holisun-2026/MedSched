package com.holisun.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record DoctorUpdateRequest(
        @NotBlank String speciality,
        @NotNull @Positive Integer standardConsultationDurationMinutes,
        @Valid List<WorkingHoursDto> schedule
) {}