package com.holisun.backend.service.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@ConditionalOnMissingBean(SmsSender.class)
public class LoggingSmsSender implements SmsSender {
    @Override
    public String send(String toE164, String body) {
        log.info("Mock SMS sent to {}: {}", toE164, body);
        return "LOG-" + UUID.randomUUID();
    }
}
