package com.holisun.backend.controller;

import com.holisun.backend.config.MethodSecurityConfig;
import com.holisun.backend.dto.report.SalesReportResponse;
import com.holisun.backend.security.JwtAuthenticationFilter;
import com.holisun.backend.service.report.ReportExcelExporter;
import com.holisun.backend.service.report.ReportPdfExporter;
import com.holisun.backend.service.report.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = ReportController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@Import(MethodSecurityConfig.class)
class ReportControllerSecurityTest {

    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private ReportExcelExporter excelExporter;

    @MockitoBean
    private ReportPdfExporter pdfExporter;

    @Test
    @WithMockUser(roles = "DOCTOR")
    void doctorCannotAccessSalesReport() throws Exception {
        mockMvc.perform(get("/api/reports/sales")
                        .param("from", "2026-07-01")
                        .param("to", "2026-07-31"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RECEPTION")
    void receptionCannotAccessSalesReport() throws Exception {
        mockMvc.perform(get("/api/reports/sales")
                        .param("from", "2026-07-01")
                        .param("to", "2026-07-31"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanAccessSalesReport() throws Exception {
        SalesReportResponse report = emptySalesReport();

        given(reportService.sales(any(), any()))
                .willReturn(report);

        mockMvc.perform(get("/api/reports/sales")
                        .param("from", "2026-07-01")
                        .param("to", "2026-07-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grandTotal").value(0));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanExportSalesAsXlsx() throws Exception {
        SalesReportResponse report = emptySalesReport();
        byte[] file = {'P', 'K', 3, 4};

        given(reportService.sales(any(), any()))
                .willReturn(report);

        given(excelExporter.exportSales(report))
                .willReturn(file);

        mockMvc.perform(get("/api/reports/sales/export")
                        .param("from", "2026-07-01")
                        .param("to", "2026-07-31")
                        .param("format", "xlsx"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(XLSX_CONTENT_TYPE))
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"raport-vanzari_2026-07-01_2026-07-31.xlsx\""
                ))
                .andExpect(content().bytes(file));
    }

    private SalesReportResponse emptySalesReport() {
        return new SalesReportResponse(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                BigDecimal.ZERO,
                List.of(),
                List.of()
        );
    }
}