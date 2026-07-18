package com.holisun.backend.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Placeholder — owner real: P2 (documents/module2/backend_module2_tasks.md, sectiunea 2).
 */
public record ConsultationRecordResponse(
        UUID id,
        UUID appointmentId,
        String presentationMotive,
        String anamnesis,
        String clinicalExam,
        String diagnosis,
        String prescription,
        boolean locked,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
