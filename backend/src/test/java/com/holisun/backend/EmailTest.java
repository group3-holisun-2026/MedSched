package com.holisun.backend;

import com.holisun.backend.entity.Notification;
import com.holisun.backend.enums.NotificationStatus;
import com.holisun.backend.enums.NotificationType;
import com.holisun.backend.repository.NotificationRepository;
import com.holisun.backend.service.notification.EmailSender;
import com.holisun.backend.service.notification.NotificationDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "app.email.enabled=true",
        "spring.mail.properties.mail.debug=true"
})
@ActiveProfiles("dev")
public class EmailTest {

    @Autowired
    private NotificationDispatcher notificationDispatcher;
    
    @Autowired
    private EmailSender emailSender;

    // We mock the repository so we don't have to create a full hierarchy of 
    // Patient -> Doctor -> Room -> Service -> Appointment just to test the email flow!
    @MockitoBean
    private NotificationRepository notificationRepository;

    @Test
    public void testFullDispatcherFlow() {
        // 1. Simulate a PENDING notification sitting in the database
        Notification mockDbNotification = new Notification();
        mockDbNotification.setId(UUID.randomUUID());
        mockDbNotification.setRecipientEmail("medsched2026@gmail.com");
        mockDbNotification.setSubject("MedSched: Dispatcher Integration Test");
        mockDbNotification.setBody("Hello! The NotificationDispatcher successfully picked this up from the database queue and sent it via JavaMail.");
        mockDbNotification.setType(NotificationType.EMAIL);
        mockDbNotification.setStatus(NotificationStatus.PENDING);
        mockDbNotification.setNextAttemptAt(LocalDateTime.now().minusMinutes(5)); // simulate it's past due

        // 2. Tell the mocked DB to return our notification when the dispatcher asks for pending items
        when(notificationRepository.findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
                any(), any()))
                .thenReturn(Collections.singletonList(mockDbNotification));

        // 3. Trigger the exact same method that runs automatically every 15 seconds
        System.out.println("=====================================================");
        System.out.println("ACTIVE EMAIL SENDER CLASS: " + emailSender.getClass().getSimpleName());
        System.out.println("Running NotificationDispatcher.dispatchPending()...");
        System.out.println("=====================================================");
        
        notificationDispatcher.dispatchPending();
        
        System.out.println("=====================================================");
        System.out.println("Dispatcher completed! Look at the console logs above.");
        System.out.println("If you see 'Mock Email sent to...', your real credentials aren't being loaded.");
        System.out.println("If you see raw SMTP server traffic (DEBUG SMTP: ...), it talked to Google.");
    }
}
