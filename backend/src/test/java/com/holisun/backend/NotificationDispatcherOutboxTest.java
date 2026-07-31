package com.holisun.backend;

import com.holisun.backend.entity.Notification;
import com.holisun.backend.enums.NotificationStatus;
import com.holisun.backend.exception.EmailPermanentException;
import com.holisun.backend.exception.EmailTransientException;
import com.holisun.backend.repository.NotificationRepository;
import com.holisun.backend.service.notification.EmailSender;
import com.holisun.backend.service.notification.NotificationDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "app.email.enabled=true")
public class NotificationDispatcherOutboxTest {

    @Autowired
    private NotificationDispatcher notificationDispatcher;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @MockitoBean
    private EmailSender emailSender;

    @Test
    public void dispatch_transientException_appliesBackoffAndRemainsPending() throws Exception {
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setStatus(NotificationStatus.PENDING);
        notification.setAttempts(0);
        notification.setRecipientEmail("test@test.com");
        notification.setSubject("Subject");
        notification.setBody("Body");

        when(notificationRepository.findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
                any(NotificationStatus.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(notification));

        // Simulate transient error
        when(emailSender.send(anyString(), anyString(), anyString()))
                .thenThrow(new EmailTransientException("Connection timeout"));

        notificationDispatcher.dispatchPending();

        assertEquals(NotificationStatus.PENDING, notification.getStatus());
        assertEquals(1, notification.getAttempts());
        assertTrue(notification.getNextAttemptAt().isAfter(LocalDateTime.now()));
        assertEquals("Connection timeout", notification.getLastError());
    }

    @Test
    public void dispatch_permanentException_failsImmediately() throws Exception {
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setStatus(NotificationStatus.PENDING);
        notification.setAttempts(0);
        notification.setRecipientEmail("test@test.com");
        notification.setSubject("Subject");
        notification.setBody("Body");

        when(notificationRepository.findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
                any(NotificationStatus.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(notification));

        // Simulate permanent error
        when(emailSender.send(anyString(), anyString(), anyString()))
                .thenThrow(new EmailPermanentException("Invalid email format"));

        notificationDispatcher.dispatchPending();

        assertEquals(NotificationStatus.FAILED, notification.getStatus());
        // Permanent exceptions shouldn't increment attempts or schedule next attempt
        assertEquals(0, notification.getAttempts());
        assertEquals("Invalid email format", notification.getLastError());
    }
}
