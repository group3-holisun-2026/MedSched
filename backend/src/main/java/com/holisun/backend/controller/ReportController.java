package com.holisun.backend.controller;

import com.holisun.backend.dto.report.NoShowReportResponse;
import com.holisun.backend.dto.report.OccupancyReportResponse;
import com.holisun.backend.dto.report.SalesReportResponse;
import com.holisun.backend.service.report.ReportExcelExporter;
import com.holisun.backend.service.report.ReportPdfExporter;
import com.holisun.backend.service.report.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Locale;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ReportController {

    private static final MediaType XLSX_MEDIA_TYPE =
            MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            );

    private final ReportService reportService;
    private final ReportExcelExporter excelExporter;
    private final ReportPdfExporter pdfExporter;

    @GetMapping("/occupancy")
    public OccupancyReportResponse occupancy(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to
    ) {
        return reportService.occupancy(from, to);
    }

    @GetMapping("/no-show")
    public NoShowReportResponse noShow(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to
    ) {
        return reportService.noShow(from, to);
    }

    @GetMapping("/sales")
    public SalesReportResponse sales(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to
    ) {
        return reportService.sales(from, to);
    }

    @GetMapping("/{type}/export")
    public ResponseEntity<byte[]> export(
            @PathVariable String type,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,

            @RequestParam String format
    ) {
        String normalizedType = type.toLowerCase(Locale.ROOT);
        String normalizedFormat = format.toLowerCase(Locale.ROOT);

        if (!normalizedFormat.equals("pdf")
                && !normalizedFormat.equals("xlsx")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Formatul trebuie sa fie pdf sau xlsx."
            );
        }

        byte[] file;
        String fileNamePrefix;

        switch (normalizedType) {
            case "occupancy" -> {
                OccupancyReportResponse report =
                        reportService.occupancy(from, to);

                file = normalizedFormat.equals("pdf")
                        ? pdfExporter.exportOccupancy(report)
                        : excelExporter.exportOccupancy(report);

                fileNamePrefix = "raport-ocupare";
            }

            case "no-show" -> {
                NoShowReportResponse report =
                        reportService.noShow(from, to);

                file = normalizedFormat.equals("pdf")
                        ? pdfExporter.exportNoShow(report)
                        : excelExporter.exportNoShow(report);

                fileNamePrefix = "raport-no-show";
            }

            case "sales" -> {
                SalesReportResponse report =
                        reportService.sales(from, to);

                file = normalizedFormat.equals("pdf")
                        ? pdfExporter.exportSales(report)
                        : excelExporter.exportSales(report);

                fileNamePrefix = "raport-vanzari";
            }

            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tipul raportului trebuie sa fie occupancy, no-show sau sales."
            );
        }

        MediaType contentType = normalizedFormat.equals("pdf")
                ? MediaType.APPLICATION_PDF
                : XLSX_MEDIA_TYPE;

        String fileName = fileNamePrefix
                + "_"
                + from
                + "_"
                + to
                + "."
                + normalizedFormat;

        return ResponseEntity.ok()
                .contentType(contentType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\""
                )
                .body(file);
    }
}