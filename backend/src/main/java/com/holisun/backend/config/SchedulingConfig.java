package com.holisun.backend.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Job-urile periodice (NotificationDispatcher la 15s, ConsultationRecordLockScheduler).
 *
 * `@EnableScheduling` statea pe BackendApplication, deci pornea si in `@SpringBootTest`: dispatcher-ul
 * real rula in fundal in timpul testelor si se bata pe aceleasi obiecte mock cu apelul manual din
 * test — de aici o flakiness reala (`dispatch_permanentException_failsImmediately` pica la o rulare
 * din doua). Mutat aici, in spatele unei proprietati, ca profilul `test` sa il poata opri.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
