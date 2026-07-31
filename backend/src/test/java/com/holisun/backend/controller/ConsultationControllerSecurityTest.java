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

import com.holisun.backend.repository.AppointmentRepository;
import com.holisun.backend.repository.DoctorRepository;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.holisun.backend.entity.Appointment;
import com.holisun.backend.entity.Doctor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Optional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

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

    @MockitoBean
    private AppointmentRepository appointmentRepository;

    @MockitoBean
    private DoctorRepository doctorRepository;

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

    @Test
    void differentDoctorCannotUpdateConsultationRecord() throws Exception {
        UUID loggedInUserId = UUID.randomUUID();

        Doctor assignedDoctor = new Doctor();
        assignedDoctor.setId(UUID.randomUUID());

        Doctor loggedInDoctor = new Doctor();
        loggedInDoctor.setId(UUID.randomUUID());

        Appointment appointment = new Appointment();
        appointment.setDoctor(assignedDoctor);

        given(appointmentRepository.findById(APPOINTMENT_ID))
                .willReturn(Optional.of(appointment));

        given(doctorRepository.findByUserId(loggedInUserId))
                .willReturn(Optional.of(loggedInDoctor));

        mockMvc.perform(put(
                        "/api/appointments/{appointmentId}/record",
                        APPOINTMENT_ID
                )
                        .with(authenticatedAs(loggedInUserId, "DOCTOR"))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    private ConsultationRecordResponse sampleResponse() {
        return new ConsultationRecordResponse(
                UUID.randomUUID(), APPOINTMENT_ID, "motiv", "anamneza", "examen clinic",
                "diagnostic", "reteta", false, LocalDateTime.now(), LocalDateTime.now()
        );
    }

    private static RequestPostProcessor authenticatedAs(
            UUID userId,
            String role
    ) {
        return authentication(
                new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_" + role)
                        )
                )
        );
    }
}
