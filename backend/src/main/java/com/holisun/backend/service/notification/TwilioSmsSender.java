package com.holisun.backend.service.notification;

import com.holisun.backend.exception.SmsPermanentException;
import com.holisun.backend.exception.SmsTransientException;
import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.sms.enabled", havingValue = "true")
public class TwilioSmsSender implements SmsSender {

    @Value("${app.sms.account-sid}")
    private String accountSid;

    @Value("${app.sms.auth-token}")
    private String authToken;

    @Value("${app.sms.from-number}")
    private String fromNumber;

    @PostConstruct
    public void init() {
        Twilio.init(accountSid, authToken);
    }

    @Override
    public String send(String toE164, String body) {
        try {
            Message message = Message.creator(
                    new PhoneNumber(toE164),
                    new PhoneNumber(fromNumber),
                    body
            ).create();
            
            return message.getSid();
            
        } catch (ApiException e) {
            int code = e.getCode();
            // Permanent errors for trial/invalid number
            // 21211: Invalid To phone number
            // 21608: This number is unverified. Trial accounts cannot send messages to unverified numbers.
            // 21610: The message violates the blackout rule / user unsubscribed
            if (code == 21211 || code == 21608 || code == 21610) {
                throw new SmsPermanentException("Permanent Twilio error: " + e.getMessage(), e);
            }
            
            // Other API exceptions (e.g., 429, 500)
            throw new SmsTransientException("Transient Twilio error: " + e.getMessage(), e);
            
        } catch (Exception e) {
            // Network or unexpected errors
            throw new SmsTransientException("Unexpected error during SMS send: " + e.getMessage(), e);
        }
    }
}
