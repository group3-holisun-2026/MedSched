package com.holisun.backend.service.notification;

import com.holisun.backend.entity.Appointment;
import com.holisun.backend.entity.Notification;
import com.holisun.backend.enums.NotificationTrigger;
import com.holisun.backend.repository.NotificationRepository;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class NotificationOutboxServiceImpl
        implements NotificationOutboxService {
    private final NotificationRepository notificationRepository;
    private final NotificationMessageFactory notificationMessageFactory;

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

    }

    @Override
    @Transactional
    public void enqueueRescheduled(Appointment appointment) {

    }

    @Override
    @Transactional
    public void enqueueCancelled(Appointment appointment) {

    }

    @Override
    @Transactional
    public void cancelPendingReminders(UUID appointmentId) {

    }
}
