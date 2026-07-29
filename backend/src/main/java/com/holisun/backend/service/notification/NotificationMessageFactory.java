package com.holisun.backend.service.notification;

import com.holisun.backend.entity.Appointment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.format.DateTimeFormatter;

@Component
public class NotificationMessageFactory {

    @Value("${app.notifications.clinic-phone}")
    private String clinicPhone;

    @Value("${app.notifications.base-url}")
    private String baseUrl;

    // Formatoare de dată și oră
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter SHORT_DATE_FMT = DateTimeFormatter.ofPattern("dd.MM");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // Șabloanele de texte (respectând NFR-1 - fără date clinice)
    private static final String CONFIRMATION_TEMPLATE =
            "MedSched: programare confirmata pentru %s, ora %s, Dr. %s, %s. Detalii la %s.";

    private static final String REMINDER_TEMPLATE =
            "MedSched: maine %s ora %s, Dr. %s, %s. Confirmati sau anulati: %s";

    private static final String RESCHEDULED_TEMPLATE =
            "MedSched: programarea a fost mutata pe %s, ora %s, Dr. %s, %s.";

    private static final String CANCELLED_TEMPLATE =
            "MedSched: programarea din %s, ora %s a fost anulata. Sunati la %s pentru reprogramare.";

    public String createConfirmationMessage(Appointment appointment) {
        return String.format(CONFIRMATION_TEMPLATE,
                DATE_FMT.format(appointment.getStartTime()),
                TIME_FMT.format(appointment.getStartTime()),
                sanitize(appointment.getDoctor().getUser().getUsername()),
                sanitize(appointment.getRoom().getName()),
                clinicPhone);
    }

    public String createReminderMessage(Appointment appointment, String confirmationToken) {
        String link = baseUrl + "/confirm?token=" + confirmationToken;
        return String.format(REMINDER_TEMPLATE,
                SHORT_DATE_FMT.format(appointment.getStartTime()),
                TIME_FMT.format(appointment.getStartTime()),
                sanitize(appointment.getDoctor().getUser().getUsername()),
                sanitize(appointment.getRoom().getName()),
                link);
    }

    public String createRescheduledMessage(Appointment appointment) {
        return String.format(RESCHEDULED_TEMPLATE,
                DATE_FMT.format(appointment.getStartTime()),
                TIME_FMT.format(appointment.getStartTime()),
                sanitize(appointment.getDoctor().getUser().getUsername()),
                sanitize(appointment.getRoom().getName()));
    }

    public String createCancelledMessage(Appointment appointment) {
        return String.format(CANCELLED_TEMPLATE,
                DATE_FMT.format(appointment.getStartTime()),
                TIME_FMT.format(appointment.getStartTime()),
                clinicPhone);
    }

    /**
     * Elimină diacriticele (ex: ă -> a, ș -> s, ț -> t) și păstrează DOAR caracterele sigure pentru GSM-7.
     */
    private String sanitize(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String noDiacritics = normalized.replaceAll("\\p{M}", "");

        return noDiacritics.replaceAll("[^A-Za-z0-9 .,:;()+\\-/@]", "");
    }
}