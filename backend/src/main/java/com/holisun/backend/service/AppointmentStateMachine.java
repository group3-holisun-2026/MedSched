package com.holisun.backend.service;

import com.holisun.backend.enums.AppointmentStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Set;

@Component
public class AppointmentStateMachine {

    private static final Map<AppointmentStatus, Set<AppointmentStatus>> ALLOWED = Map.of(
            AppointmentStatus.SCHEDULED,   Set.of(AppointmentStatus.CONFIRMED, AppointmentStatus.CANCELLED),
            AppointmentStatus.CONFIRMED,   Set.of(AppointmentStatus.IN_PROGRESS, AppointmentStatus.NO_SHOW, AppointmentStatus.CANCELLED),
            AppointmentStatus.IN_PROGRESS, Set.of(AppointmentStatus.COMPLETED),
            AppointmentStatus.COMPLETED,   Set.of(),
            AppointmentStatus.NO_SHOW,     Set.of(),
            AppointmentStatus.CANCELLED,   Set.of()
    );

    public void assertTransition(AppointmentStatus from, AppointmentStatus to) {
        if (!ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Tranziția din " + from + " în " + to + " nu este permisă.");
        }
    }
}