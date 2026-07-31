package com.holisun.backend.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SalesReportResponse(
        LocalDate from,
        LocalDate to,
        BigDecimal grandTotal,
        List<SalesRow> byService,
        List<SalesRow> byDoctor
) {
}