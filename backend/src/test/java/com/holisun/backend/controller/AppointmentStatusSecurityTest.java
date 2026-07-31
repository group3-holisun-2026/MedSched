package com.holisun.backend.controller;

import com.holisun.backend.config.MethodSecurityConfig;
import com.holisun.backend.entity.Appointment;
import com.holisun.backend.entity.Doctor;
import com.holisun.backend.repository.AppointmentRepository;
import com.holisun.backend.repository.DoctorRepository;
import com.holisun.backend.security.JwtAuthenticationFilter;
import com.holisun.backend.service.AppointmentService;
import com.holisun.backend.service.CalendarService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cerinta #2 din Definition of Done (documents/module4/backend_module4_tasks.md, sectiunea 3):
 * masina de stari nu poate fi ocolita prin apeluri API neautorizate. Verifica strict tabelul
 * de roluri din sectiunea 1.2 — inclusiv restrictia "doar medicul alocat" pentru /complete,
 * care se evalueaza in controller (nu la nivel de filtru Spring Security).
 */
@WebMvcTest(
        controllers = AppointmentController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@Import(MethodSecurityConfig.class)
class AppointmentStatusSecurityTest {

    private static final UUID APPOINTMENT_ID = UUID.randomUUID();
    private static final String BASE_URL = "/api/appointments/" + APPOINTMENT_ID;

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AppointmentService appointmentService;
    @MockitoBean private CalendarService calendarService;
    @MockitoBean private DoctorRepository doctorRepository;
    @MockitoBean private AppointmentRepository appointmentRepository;

    @Test
    @WithMockUser(roles = "RECEPTION")
    void receptionCannotCompleteAppointment() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/complete").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RECEPTION")
    void receptionCanConfirmAppointment() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/confirm").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "RECEPTION")
    void receptionCanMarkNoShow() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/no-show").with(csrf()))
                .andExpect(status().isOk());
    }

    /**
     * Un DOCTOR valid, dar care nu e medicul alocat programarii, primeste 403 — nu are voie
     * sa "inchida" un consult pe care nu l-a facut el (sectiunea 1.2, motivatia deciziei).
     */
    @Test
    void otherDoctorCannotCompleteAppointment() throws Exception {
        UUID loggedInUserId = UUID.randomUUID();

        Doctor assignedDoctor = doctorWithId(UUID.randomUUID());
        Doctor loggedInDoctor = doctorWithId(UUID.randomUUID());

        given(appointmentRepository.findById(APPOINTMENT_ID))
                .willReturn(Optional.of(appointmentOf(assignedDoctor)));
        given(doctorRepository.findByUserId(loggedInUserId))
                .willReturn(Optional.of(loggedInDoctor));

        mockMvc.perform(patch(BASE_URL + "/complete").with(csrf()).with(authenticatedAs(loggedInUserId, "DOCTOR")))
                .andExpect(status().isForbidden());

        verify(appointmentService, never()).complete(APPOINTMENT_ID, loggedInDoctor.getId());
    }

    @Test
    void assignedDoctorCanCompleteAppointment() throws Exception {
        UUID loggedInUserId = UUID.randomUUID();
        UUID assignedDoctorId = UUID.randomUUID();

        Doctor assignedDoctor = doctorWithId(assignedDoctorId);

        given(appointmentRepository.findById(APPOINTMENT_ID))
                .willReturn(Optional.of(appointmentOf(assignedDoctor)));
        given(doctorRepository.findByUserId(loggedInUserId))
                .willReturn(Optional.of(assignedDoctor));

        mockMvc.perform(patch(BASE_URL + "/complete").with(csrf()).with(authenticatedAs(loggedInUserId, "DOCTOR")))
                .andExpect(status().isOk());

        verify(appointmentService).complete(APPOINTMENT_ID, assignedDoctorId);
    }

    private static Doctor doctorWithId(UUID id) {
        Doctor doctor = new Doctor();
        doctor.setId(id);
        return doctor;
    }

    private static Appointment appointmentOf(Doctor doctor) {
        Appointment appointment = new Appointment();
        appointment.setId(APPOINTMENT_ID);
        appointment.setDoctor(doctor);
        return appointment;
    }

    private static RequestPostProcessor authenticatedAs(UUID userId, String role) {
        return authentication(new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
}
