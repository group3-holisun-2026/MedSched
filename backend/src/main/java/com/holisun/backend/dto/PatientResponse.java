package com.holisun.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Placeholder — owner real: P1 (documents/module2/backend_module2_tasks.md, sectiunea 1).
 */
public record PatientResponse(
        UUID id,
        String firstName,
        String lastName,
        String phone,
        String cnp,
        LocalDate dateOfBirth,
        String email,
        String allergies,
        String medicalHistory,
        boolean profileComplete,
        LocalDateTime createdAt
) {
}
