package com.holisun.backend.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record WorkingHoursDto(
        @NotNull Short dayOfWeek,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime
) {}
