package com.holisun.backend.dto;

/**
 * Placeholder — owner real: P2 (documents/module2/backend_module2_tasks.md, sectiunea 2).
 */
public record ConsultationRecordRequest(
        String presentationMotive,
        String anamnesis,
        String clinicalExam,
        String diagnosis,
        String prescription
) {
}
