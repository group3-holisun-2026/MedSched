package com.holisun.backend.dto;

import com.holisun.backend.entity.Notification;
import com.holisun.backend.enums.NotificationStatus;
import com.holisun.backend.enums.NotificationTrigger;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID appointmentId,
        NotificationTrigger trigger,
        NotificationStatus status,
        String recipientEmail,
        String body,
        int attempts,
        String lastError,
        LocalDateTime nextAttemptAt,
        LocalDateTime sentAt,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification notification) {
        String maskedEmail = maskEmail(notification.getRecipientEmail());
        return new NotificationResponse(
                notification.getId(),
                notification.getAppointment().getId(),
                notification.getTrigger(),
                notification.getStatus(),
                maskedEmail,
                notification.getBody(),
                notification.getAttempts(),
                notification.getLastError(),
                notification.getNextAttemptAt(),
                notification.getSentAt(),
                notification.getCreatedAt()
        );
    }

    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int atIndex = email.indexOf("@");
        String username = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        
        if (username.length() <= 1) {
            return "***" + domain;
        }
        
        return username.charAt(0) + "***" + domain;
    }
}
