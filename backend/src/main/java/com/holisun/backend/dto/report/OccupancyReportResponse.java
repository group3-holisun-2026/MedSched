package com.holisun.backend.dto.report;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record OccupancyReportResponse(
        LocalDate from,
        LocalDate to,
        List<ResourceOccupancyRow> doctors,
        List<ResourceOccupancyRow> rooms
) {

}
