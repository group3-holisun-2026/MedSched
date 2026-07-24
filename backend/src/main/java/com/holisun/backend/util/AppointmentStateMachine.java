package com.holisun.backend.util;

import com.holisun.backend.enums.AppointmentStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Set;

import static com.holisun.backend.enums.AppointmentStatus.*;

@Component
public class AppointmentStateMachine {
    private static final Map<AppointmentStatus, Set<AppointmentStatus>> ALLOWED = Map.of(
            SCHEDULED,   Set.of(CONFIRMED, CANCELLED),
            CONFIRMED,   Set.of(IN_PROGRESS, NO_SHOW, CANCELLED),
            IN_PROGRESS, Set.of(COMPLETED),
            COMPLETED,   Set.of(),
            NO_SHOW,     Set.of(),
            CANCELLED,   Set.of()
    );

    public void assertTransition(AppointmentStatus from, AppointmentStatus to) {
        if (!ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Tranziția din " + from + " în " + to + " nu este permisă.");
        }
    }
}
