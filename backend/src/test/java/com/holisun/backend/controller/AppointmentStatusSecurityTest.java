package com.holisun.backend.controller;

import com.holisun.backend.service.AppointmentService;
import org.junit.jupiter.api.Test;
import com.holisun.backend.service.CalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import com.holisun.backend.repository.DoctorRepository;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AppointmentController.class)
public class AppointmentStatusSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    // Mockuim serviciile ca să nu pornească toată baza de date, testăm strict securitatea (filtrele)
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private AppointmentService appointmentService;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private CalendarService calendarService;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private DoctorRepository doctorRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.holisun.backend.security.JwtUtil jwtUtil;

    private final String BASE_URL = "/api/appointments/" + UUID.randomUUID();

    // 1. RECEPTION pe /complete -> 403 Forbidden
    @Test
    @WithMockUser(roles = "RECEPTION")
    void givenReception_whenComplete_thenForbidden() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/complete").with(csrf()))
                .andExpect(status().isForbidden());
    }

    // 2. RECEPTION pe /confirm -> 200 OK
    @Test
    @WithMockUser(roles = "RECEPTION")
    void givenReception_whenConfirm_thenOk() throws Exception {
        // Presupunând că endpoint-ul va întoarce 200 OK dacă securitatea trece
        // Dacă P5 nu a creat încă endpoint-ul, testul va da 404 (Not Found), ceea ce ne arată că a trecut de 403/401!
        mockMvc.perform(patch(BASE_URL + "/confirm").with(csrf()))
                .andExpect(status().isOk());
    }

    // 3. RECEPTION pe /no-show -> 200 OK
    @Test
    @WithMockUser(roles = "RECEPTION")
    void givenReception_whenNoShow_thenOk() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/no-show").with(csrf()))
                .andExpect(status().isOk());
    }

    // Notă pentru DOCTOR:
    // Testarea logicii de "doar medicul alocat" va necesita ca P5 (controller-ul) să aibă logica scrisă
    // deoarece acea verificare (403 Custom) se face în interiorul metodei din Controller, nu la nivel de filtru Spring Security.
}

