package com.holisun.backend.dto;

import com.holisun.backend.dto.summary.*;
import com.holisun.backend.enums.AppointmentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentResponse(
        UUID id,
        PatientSummary patient,
        DoctorSummary doctor,
        RoomSummary room,
        ServiceSummary service,
        EquipmentSummary equipment,
        LocalDateTime startTime,
        LocalDateTime endTime,
        AppointmentStatus status,
        String notes,
        long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
