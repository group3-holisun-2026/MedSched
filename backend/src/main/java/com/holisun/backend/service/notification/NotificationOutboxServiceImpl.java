package com.holisun.backend.service.notification;

import com.holisun.backend.entity.Appointment;
import com.holisun.backend.entity.Notification;
import com.holisun.backend.enums.NotificationStatus;
import com.holisun.backend.enums.NotificationTrigger;
import com.holisun.backend.enums.NotificationType;
import com.holisun.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationOutboxServiceImpl implements NotificationOutboxService {

    private final NotificationRepository notificationRepository;
    private final NotificationMessageFactory notificationMessageFactory;

    private static final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public void enqueueConfirmation(Appointment appointment) {
        EmailContent content = notificationMessageFactory.createConfirmationMessage(appointment);
        Notification notification = buildBaseNotification(appointment, NotificationTrigger.CONFIRMATION, content, LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void enqueueReminder(Appointment appointment) {
        LocalDateTime nextAttemptAt = appointment.getStartTime().minusHours(24);
        if (nextAttemptAt.isBefore(LocalDateTime.now()) || nextAttemptAt.isEqual(LocalDateTime.now())) {
            return;
        }

        byte[] randomBytes = new byte[12];
        secureRandom.nextBytes(randomBytes);
        String confirmationToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        EmailContent content = notificationMessageFactory.createReminderMessage(appointment, confirmationToken);
        Notification notification = buildBaseNotification(appointment, NotificationTrigger.REMINDER_24H, content, nextAttemptAt);
        notification.setConfirmationToken(confirmationToken);
        
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void enqueueRescheduled(Appointment appointment) {
        cancelPendingReminders(appointment.getId());

        EmailContent content = notificationMessageFactory.createRescheduledMessage(appointment);
        Notification notification = buildBaseNotification(appointment, NotificationTrigger.RESCHEDULED, content, LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void enqueueCancelled(Appointment appointment) {
        cancelPendingReminders(appointment.getId());

        EmailContent content = notificationMessageFactory.createCancelledMessage(appointment);
        Notification notification = buildBaseNotification(appointment, NotificationTrigger.CANCELLED, content, LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void cancelPendingReminders(UUID appointmentId) {
        List<Notification> pendingReminders = notificationRepository.findByAppointmentIdAndTriggerAndStatus(
                appointmentId, NotificationTrigger.REMINDER_24H, NotificationStatus.PENDING);
        
        for (Notification reminder : pendingReminders) {
            reminder.setStatus(NotificationStatus.CANCELLED);
            notificationRepository.save(reminder);
        }
    }

    private Notification buildBaseNotification(Appointment appointment, NotificationTrigger trigger, EmailContent content, LocalDateTime nextAttemptAt) {
        Notification notification = new Notification();
        notification.setAppointment(appointment);
        notification.setType(NotificationType.EMAIL);
        notification.setTrigger(trigger);
        notification.setSubject(content.subject());
        notification.setBody(content.body());
        notification.setNextAttemptAt(nextAttemptAt);

        String email = appointment.getPatient().getEmail();
        if (email == null || email.isBlank() || !email.contains("@")) {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setLastError("Invalid email address: " + email);
        } else {
            notification.setRecipientEmail(email);
            notification.setStatus(NotificationStatus.PENDING);
        }

        return notification;
    }
}
