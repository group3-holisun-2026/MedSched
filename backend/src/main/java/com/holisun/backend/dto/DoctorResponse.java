package com.holisun.backend.dto;

import java.util.List;
import java.util.UUID;

public record DoctorResponse(
        UUID id,
        UUID userId,
        String fullName,
        String speciality,
        /** `GET /api/doctors` intoarce si medicii dezactivati, deci consumatorii au nevoie de
         *  steag ca sa poata filtra — filtrul de calendar arata doar medicii activi. */
        Boolean active,
        Integer standardConsultationDurationMinutes,
        List<WorkingHoursDto> schedule
) {}
