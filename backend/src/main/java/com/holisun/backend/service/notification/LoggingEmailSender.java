package com.holisun.backend.service.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.email.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingEmailSender implements EmailSender {

    @Override
    public String send(String toEmail, String subject, String body) {
        String mockProviderId = "mock-email-" + UUID.randomUUID().toString().substring(0, 8);
        log.info("Mock Email sent to {}: Subject: [{}] | Body: {}", toEmail, subject, body);
        return mockProviderId;
    }
}
