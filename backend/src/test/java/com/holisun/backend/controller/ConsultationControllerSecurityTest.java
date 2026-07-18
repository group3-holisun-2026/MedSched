package com.holisun.backend.controller;

import com.holisun.backend.config.MethodSecurityConfig;
import com.holisun.backend.dto.ConsultationRecordResponse;
import com.holisun.backend.security.JwtAuthenticationFilter;
import com.holisun.backend.service.ConsultationRecordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Criteriul de acceptanta #3 (F-202): un RECEPTION nu poate accesa fisa de consultatie
 * (403), un DOCTOR/ADMIN poate. Foloseste doar slice-ul web + MethodSecurityConfig — nu
 * are nevoie de baza de date sau de filtrul JWT real (@WithMockUser populeaza direct
 * SecurityContext-ul).
 */
@WebMvcTest(
        controllers = ConsultationController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@Import(MethodSecurityConfig.class)
class ConsultationControllerSecurityTest {

    private static final UUID APPOINTMENT_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConsultationRecordService consultationRecordService;

    @Test
    @WithMockUser(roles = "RECEPTION")
    void receptionCannotAccessConsultationRecord() throws Exception {
        mockMvc.perform(get("/api/appointments/{appointmentId}/record", APPOINTMENT_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DOCTOR")
    void doctorCanAccessConsultationRecord() throws Exception {
        given(consultationRecordService.getByAppointmentId(APPOINTMENT_ID)).willReturn(sampleResponse());

        mockMvc.perform(get("/api/appointments/{appointmentId}/record", APPOINTMENT_ID))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanAccessConsultationRecord() throws Exception {
        given(consultationRecordService.getByAppointmentId(APPOINTMENT_ID)).willReturn(sampleResponse());

        mockMvc.perform(get("/api/appointments/{appointmentId}/record", APPOINTMENT_ID))
                .andExpect(status().isOk());
    }

    private ConsultationRecordResponse sampleResponse() {
        return new ConsultationRecordResponse(
                UUID.randomUUID(), APPOINTMENT_ID, "motiv", "anamneza", "examen clinic",
                "diagnostic", "reteta", false, LocalDateTime.now(), LocalDateTime.now()
        );
    }
}
