package com.holisun.backend.dto.report;

import java.math.BigDecimal;
import java.util.UUID;

public record SalesRow(
        UUID id,
        String name,
        long appointments,
        BigDecimal total
) {
}