package com.holisun.backend.dto.report;
import java.util.UUID;

public record ResourceOccupancyRow(
        UUID resourceId,
        String name,
        long bookedMinutes,
        long availableMinutes,
        double occupancyRate
) {

}
