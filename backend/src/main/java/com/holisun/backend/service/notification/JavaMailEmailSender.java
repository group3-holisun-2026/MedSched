package com.holisun.backend.service.notification;

import com.holisun.backend.exception.EmailPermanentException;
import com.holisun.backend.exception.EmailTransientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.email.enabled", havingValue = "true")
public class JavaMailEmailSender implements EmailSender {

    private final JavaMailSender javaMailSender;

    @Value("${app.email.from-address}")
    private String fromAddress;

    @Override
    public String send(String toEmail, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);

            javaMailSender.send(message);

            // JavaMail doesn't usually return an exact message ID synchronously that we can use, 
            // so we generate one or return null.
            return "javamail-" + UUID.randomUUID().toString();

        } catch (MailAuthenticationException e) {
            log.error("Failed to authenticate with SMTP server", e);
            throw new EmailPermanentException("SMTP Authentication failed: " + e.getMessage(), e);
        } catch (MailSendException e) {
            log.warn("Transient mail send exception", e);
            throw new EmailTransientException("Failed to send email: " + e.getMessage(), e);
        } catch (MailException e) {
            log.warn("Generic mail exception", e);
            throw new EmailTransientException("Mail exception: " + e.getMessage(), e);
        }
    }
}
