package com.holisun.backend.dto.report;

import java.time.DayOfWeek;

public record WeekdayNoShowRow(
        DayOfWeek dayOfWeek,
        long total,
        long noShows,
        double rate
) {
}