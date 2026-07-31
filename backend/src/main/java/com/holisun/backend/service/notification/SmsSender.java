package com.holisun.backend.service.notification;

public interface SmsSender {
    /** @return id-ul mesajului la provider (SID la Twilio) */
    String send(String toE164, String body);
}
