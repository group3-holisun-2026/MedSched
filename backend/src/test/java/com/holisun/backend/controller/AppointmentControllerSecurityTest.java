package com.holisun.backend.controller;

import com.holisun.backend.config.MethodSecurityConfig;
import com.holisun.backend.dto.CalendarAppointmentResponse;
import com.holisun.backend.entity.Doctor;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AppointmentController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@Import(MethodSecurityConfig.class)
class AppointmentControllerSecurityTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AppointmentService appointmentService;
    @MockitoBean private CalendarService calendarService;
    @MockitoBean private DoctorRepository doctorRepository;

    @Test
    @WithMockUser(roles = "DOCTOR")
    void doctorCannotCreateAppointment() throws Exception {
        mockMvc.perform(post("/api/appointments")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void doctorCalendarRequestIgnoresRequestedDoctorIdsAndUsesOwnDoctorId() throws Exception {
        UUID loggedInUserId = UUID.randomUUID();
        UUID ownDoctorId = UUID.randomUUID();
        UUID otherDoctorId = UUID.randomUUID();

        Doctor self = new Doctor();
        self.setId(ownDoctorId);
        given(doctorRepository.findByUserId(loggedInUserId)).willReturn(Optional.of(self));
        given(calendarService.getForDoctor(eq(ownDoctorId), any(), any()))
                .willReturn(List.of(new CalendarAppointmentResponse()));

        mockMvc.perform(get("/api/appointments/calendar")
                        .param("from", "2026-07-23T00:00:00")
                        .param("to", "2026-07-24T00:00:00")
                        .param("doctorIds", otherDoctorId.toString())
                        .with(authenticatedAs(loggedInUserId, "DOCTOR")))
                .andExpect(status().isOk());

        verify(calendarService).getForDoctor(eq(ownDoctorId), any(), any());
        verify(calendarService, never()).getByDateRangeAndDoctors(any(), any(), any());
    }

    private static RequestPostProcessor authenticatedAs(UUID userId, String role) {
        return authentication(new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
}
