package com.holisun.backend.service.notification;

public interface EmailSender {
    /**
     * @param toEmail The recipient email address
     * @param subject The email subject
     * @param body The email body
     * @return providerId if any, else a generated identifier or simply null
     */
    String send(String toEmail, String subject, String body);
}
