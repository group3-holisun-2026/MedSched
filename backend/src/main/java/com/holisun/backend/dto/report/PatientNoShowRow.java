package com.holisun.backend.dto.report;

import java.util.UUID;

public record PatientNoShowRow(
        UUID patientId,
        String patientName,
        long total,
        long noShows,
        double rate
) {

}