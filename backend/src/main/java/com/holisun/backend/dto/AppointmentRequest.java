package com.holisun.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentRequest(
    @NotNull
    UUID patientId,
    @NotNull
    UUID doctorId,
    @NotNull
    UUID roomId,
    @NotNull
    UUID serviceId,
    @NotNull
    LocalDateTime startTime,
    String notes
){}
