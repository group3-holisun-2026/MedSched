package com.holisun.backend.controller;

import com.holisun.backend.config.MethodSecurityConfig;
import com.holisun.backend.entity.Notification;
import com.holisun.backend.enums.NotificationStatus;
import com.holisun.backend.repository.NotificationRepository;
import com.holisun.backend.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Coada de notificari expune corpul mesajelor si adresele pacientilor, deci e ADMIN-only (NFR-1).
 * Acelasi tipar ca {@link ReportControllerSecurityTest}.
 */
@WebMvcTest(
        controllers = NotificationAdminController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@Import(MethodSecurityConfig.class)
class NotificationAdminControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @Test
    @WithMockUser(roles = "DOCTOR")
    void doctorCannotListNotifications() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RECEPTION")
    void receptionCannotListNotifications() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RECEPTION")
    void receptionCannotRetryNotification() throws Exception {
        mockMvc.perform(post("/api/notifications/{id}/retry", UUID.randomUUID()).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanListNotifications() throws Exception {
        given(notificationRepository.findAllByOrderByCreatedAtDesc(any()))
                .willReturn(Page.empty());

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void retryOnNonFailedNotificationIsRejected() throws Exception {
        Notification sent = new Notification();
        sent.setId(UUID.randomUUID());
        sent.setStatus(NotificationStatus.SENT);

        given(notificationRepository.findById(any())).willReturn(Optional.of(sent));

        mockMvc.perform(post("/api/notifications/{id}/retry", sent.getId()).with(csrf()))
                .andExpect(status().isConflict());
    }
}
