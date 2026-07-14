package com.holisun.backend.dto;

import java.util.List;
import java.util.UUID;

public record DoctorResponse(
        UUID id,
        UUID userId,
        String fullName,
        String speciality,
        Integer standardConsultationDurationMinutes,
        List<WorkingHoursDto> schedule
) {}