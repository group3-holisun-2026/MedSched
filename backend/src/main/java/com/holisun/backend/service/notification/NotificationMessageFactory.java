package com.holisun.backend.service.notification;

import com.holisun.backend.entity.Appointment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.format.DateTimeFormatter;

@Component
public class NotificationMessageFactory {



    @Value("${app.notifications.public-base-url}")
    private String baseUrl;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter SHORT_DATE_FMT = DateTimeFormatter.ofPattern("dd.MM");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private static final String CONFIRMATION_SUBJECT = "MedSched: Programare confirmata";
    private static final String CONFIRMATION_BODY = 
            "Programare confirmata pentru %s, ora %s, Dr. %s, %s. Detalii la Medsched2026@gmail.com.";

    private static final String REMINDER_SUBJECT = "MedSched: Reminder programare maine";
    private static final String REMINDER_BODY = 
            "Maine %s ora %s, Dr. %s, %s. Confirmati sau anulati: %s";

    private static final String RESCHEDULED_SUBJECT = "MedSched: Programare mutata";
    private static final String RESCHEDULED_BODY = 
            "Programarea a fost mutata pe %s, ora %s, Dr. %s, %s.";

    private static final String CANCELLED_SUBJECT = "MedSched: Programare anulata";
    private static final String CANCELLED_BODY = 
            "Programarea din %s, ora %s a fost anulata. Trimiteti un mail la Medsched2026@gmail.com pentru reprogramare.";

    public EmailContent createConfirmationMessage(Appointment appointment) {
        String body = String.format(CONFIRMATION_BODY,
                DATE_FMT.format(appointment.getStartTime()),
                TIME_FMT.format(appointment.getStartTime()),
                sanitize(appointment.getDoctor().getUser().getUsername()),
                sanitize(appointment.getRoom().getName()));
        return new EmailContent(CONFIRMATION_SUBJECT, body);
    }

    public EmailContent createReminderMessage(Appointment appointment, String confirmationToken) {
        String link = baseUrl + "/confirm?token=" + confirmationToken;
        String body = String.format(REMINDER_BODY,
                SHORT_DATE_FMT.format(appointment.getStartTime()),
                TIME_FMT.format(appointment.getStartTime()),
                sanitize(appointment.getDoctor().getUser().getUsername()),
                sanitize(appointment.getRoom().getName()),
                link);
        return new EmailContent(REMINDER_SUBJECT, body);
    }

    public EmailContent createRescheduledMessage(Appointment appointment) {
        String body = String.format(RESCHEDULED_BODY,
                DATE_FMT.format(appointment.getStartTime()),
                TIME_FMT.format(appointment.getStartTime()),
                sanitize(appointment.getDoctor().getUser().getUsername()),
                sanitize(appointment.getRoom().getName()));
        return new EmailContent(RESCHEDULED_SUBJECT, body);
    }

    public EmailContent createCancelledMessage(Appointment appointment) {
        String body = String.format(CANCELLED_BODY,
                DATE_FMT.format(appointment.getStartTime()),
                TIME_FMT.format(appointment.getStartTime()));
        return new EmailContent(CANCELLED_SUBJECT, body);
    }

    private String sanitize(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String noDiacritics = normalized.replaceAll("\\p{M}", "");
        return noDiacritics.replaceAll("[^A-Za-z0-9 .,:;()+\\-/@]", "");
    }
}