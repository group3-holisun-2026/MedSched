package com.holisun.backend;

import com.holisun.backend.entity.Notification;
import com.holisun.backend.enums.NotificationStatus;
import com.holisun.backend.enums.NotificationType;
import com.holisun.backend.repository.NotificationRepository;
import com.holisun.backend.service.notification.EmailSender;
import com.holisun.backend.service.notification.LoggingEmailSender;
import com.holisun.backend.service.notification.NotificationDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Fluxul complet al dispatcher-ului pe calea fericita: un rand PENDING scadent e preluat si trimis.
 *
 * Ruleaza cu kill switch-ul pe OFF (`app.email.enabled` implicit false), deci bean-ul activ e
 * {@link LoggingEmailSender} — testul nu deschide nicio conexiune SMTP si nu are nevoie de
 * credentiale. Varianta initiala pornea cu `enabled=true` pe profilul `dev` si chiar incerca sa
 * contacteze smtp.gmail.com la fiecare rulare a suitei, fara sa asserteze nimic.
 *
 * Comportamentul la esec (retry, backoff, NFR-2) e testat in {@link NotificationDispatcherOutboxTest}.
 */
@ActiveProfiles("test")
@SpringBootTest
public class EmailTest {

    @Autowired
    private NotificationDispatcher notificationDispatcher;

    @Autowired
    private EmailSender emailSender;

    // Mockam repository-ul ca sa nu construim tot lantul Patient -> Doctor -> Room -> Service
    // -> Appointment doar pentru a verifica dispatcher-ul.
    @MockitoBean
    private NotificationRepository notificationRepository;

    @Test
    public void dispatcherPicksUpADueNotificationAndMarksItSent() {
        Notification pending = new Notification();
        pending.setId(UUID.randomUUID());
        pending.setRecipientEmail("pacient@exemplu.ro");
        pending.setSubject("MedSched: test dispatcher");
        pending.setBody("Corp de test.");
        pending.setType(NotificationType.EMAIL);
        pending.setStatus(NotificationStatus.PENDING);
        pending.setNextAttemptAt(LocalDateTime.now().minusMinutes(5)); // deja scadenta

        when(notificationRepository.findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
                any(), any()))
                .thenReturn(Collections.singletonList(pending));

        notificationDispatcher.dispatchPending();

        assertThat(emailSender).isInstanceOf(LoggingEmailSender.class);
        assertThat(pending.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(pending.getSentAt()).isNotNull();
        assertThat(pending.getProviderMessageId()).isNotBlank();
        assertThat(pending.getLastError()).isNull();
    }
}
