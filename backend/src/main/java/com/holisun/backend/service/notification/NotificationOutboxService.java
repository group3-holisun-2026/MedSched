package com.holisun.backend.service.notification;

import com.holisun.backend.entity.Appointment;

import java.util.UUID;

public interface NotificationOutboxService {

    void enqueueConfirmation(Appointment appointment);

    void enqueueReminder(Appointment appointment);

    void enqueueRescheduled(Appointment appointment);

    void enqueueCancelled(Appointment appointment);

    void cancelPendingReminders(UUID appointmentId);
}
