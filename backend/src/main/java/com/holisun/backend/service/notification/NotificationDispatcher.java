package com.holisun.backend.service.notification;

import com.holisun.backend.entity.Notification;
import com.holisun.backend.enums.NotificationStatus;
import com.holisun.backend.exception.EmailPermanentException;
import com.holisun.backend.exception.EmailTransientException;
import com.holisun.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDispatcher {

    private final NotificationRepository notificationRepository;
    private final EmailSender emailSender;

    private static final int[] BACKOFF = {1, 5, 15, 60, 360}; // in minutes

    @Scheduled(fixedDelay = 15_000)
    public void dispatchPending() {
        List<Notification> pending = notificationRepository
                .findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
                        NotificationStatus.PENDING, LocalDateTime.now());

        if (pending.isEmpty()) {
            return;
        }

        for (Notification notification : pending) {
            try {
                // Try to send the Email
                String providerId = emailSender.send(notification.getRecipientEmail(), notification.getSubject(), notification.getBody());
                
                // Success
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
                notification.setProviderMessageId(providerId);
                notification.setLastError(null);
                
                notificationRepository.save(notification);

            } catch (EmailPermanentException e) {
                // Permanent failure (e.g. auth failed), do not retry
                log.warn("Permanent Email failure for notification {}: {}", notification.getId(), e.getMessage());
                notification.setStatus(NotificationStatus.FAILED);
                notification.setLastError(e.getMessage());
                notificationRepository.save(notification);

            } catch (EmailTransientException e) {
                // Transient failure (e.g. timeout), apply backoff and retry
                handleTransientFailure(notification, e);
            } catch (Exception e) {
                // Unexpected runtime exception, treat as transient
                handleTransientFailure(notification, e);
            }
        }
    }

    private void handleTransientFailure(Notification notification, Exception e) {
        log.warn("Transient Email failure for notification {}: {}", notification.getId(), e.getMessage());
        int attempts = notification.getAttempts() + 1;
        notification.setAttempts(attempts);

        if (attempts >= 5) {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setLastError("Max attempts reached. Last error: " + e.getMessage());
        } else {
            int backoffMinutes = BACKOFF[attempts - 1];
            notification.setNextAttemptAt(LocalDateTime.now().plusMinutes(backoffMinutes));
            notification.setLastError(e.getMessage());
        }
        
        notificationRepository.save(notification);
    }
}
