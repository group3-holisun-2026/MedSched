package com.holisun.backend.service.notification;

import com.holisun.backend.entity.Appointment;
import com.holisun.backend.entity.Notification;
import com.holisun.backend.enums.NotificationStatus;
import com.holisun.backend.enums.NotificationTrigger;
import com.holisun.backend.enums.NotificationType;
import com.holisun.backend.repository.NotificationRepository;
import com.holisun.backend.util.PhoneNumberNormalizer;
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
    private final PhoneNumberNormalizer phoneNumberNormalizer;

    private static final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public void enqueueConfirmation(Appointment appointment) {
        String body = notificationMessageFactory.createConfirmationMessage(appointment);
        Notification notification = buildBaseNotification(appointment, NotificationTrigger.CONFIRMATION, body, LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void enqueueReminder(Appointment appointment) {
        LocalDateTime nextAttemptAt = appointment.getStartTime().minusHours(24);
        if (nextAttemptAt.isBefore(LocalDateTime.now()) || nextAttemptAt.isEqual(LocalDateTime.now())) {
            return; // nu face nimic dacă startTime - 24h <= now()
        }

        // Token: 16 caractere URL-safe, generat cu SecureRandom (12 bytes -> Base64 URL-safe fără padding)
        byte[] randomBytes = new byte[12];
        secureRandom.nextBytes(randomBytes);
        String confirmationToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        String body = notificationMessageFactory.createReminderMessage(appointment, confirmationToken);
        Notification notification = buildBaseNotification(appointment, NotificationTrigger.REMINDER_24H, body, nextAttemptAt);
        notification.setConfirmationToken(confirmationToken);
        
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void enqueueRescheduled(Appointment appointment) {
        cancelPendingReminders(appointment.getId());

        String body = notificationMessageFactory.createRescheduledMessage(appointment);
        Notification notification = buildBaseNotification(appointment, NotificationTrigger.RESCHEDULED, body, LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void enqueueCancelled(Appointment appointment) {
        cancelPendingReminders(appointment.getId());

        String body = notificationMessageFactory.createCancelledMessage(appointment);
        Notification notification = buildBaseNotification(appointment, NotificationTrigger.CANCELLED, body, LocalDateTime.now());
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

    private Notification buildBaseNotification(Appointment appointment, NotificationTrigger trigger, String body, LocalDateTime nextAttemptAt) {
        Notification notification = new Notification();
        notification.setAppointment(appointment);
        notification.setType(NotificationType.SMS);
        notification.setTrigger(trigger);
        notification.setBody(body);
        notification.setNextAttemptAt(nextAttemptAt);

        try {
            String phone = phoneNumberNormalizer.normalizePhoneNumber(appointment.getPatient().getPhone());
            notification.setRecipientPhone(phone);
            notification.setStatus(NotificationStatus.PENDING);
        } catch (IllegalArgumentException e) {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setLastError("Invalid phone number: " + e.getMessage());
        }

        return notification;
    }
}
