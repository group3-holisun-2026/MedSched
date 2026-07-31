package com.holisun.backend.dto.report;

import java.time.LocalDate;
import java.util.List;

public record NoShowReportResponse(
        LocalDate from,
        LocalDate to,
        long total,
        long noShows,
        double rate,
        List<PatientNoShowRow> byPatient,
        List<WeekdayNoShowRow> byWeekday
) {
}