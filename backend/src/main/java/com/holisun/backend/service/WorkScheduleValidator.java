package com.holisun.backend.service;

import com.holisun.backend.dto.WorkingHoursDto;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Verifica faptul ca intervalele orare dintr-un program saptamanal trimis de client
 * nu se suprapun in aceeasi zi. Nu verifica conflicte fata de programari existente
 * (asta e responsabilitatea viitorului AvailabilityValidatorService, la nivel de appointment).
 */
@Component
public class WorkScheduleValidator {

    public void validateNoOverlaps(List<WorkingHoursDto> schedule) {
        if (schedule == null || schedule.isEmpty()) {
            return;
        }

        Map<Short, List<WorkingHoursDto>> byDay = schedule.stream()
                .collect(Collectors.groupingBy(WorkingHoursDto::dayOfWeek));

        for (List<WorkingHoursDto> dayEntries : byDay.values()) {
            List<WorkingHoursDto> sorted = dayEntries.stream()
                    .sorted(Comparator.comparing(WorkingHoursDto::startTime))
                    .toList();

            for (int i = 1; i < sorted.size(); i++) {
                WorkingHoursDto previous = sorted.get(i - 1);
                WorkingHoursDto current = sorted.get(i);
                if (current.startTime().isBefore(previous.endTime())) {
                    throw new IllegalArgumentException(
                            "Intervalele orare se suprapun in ziua " + current.dayOfWeek() + ": "
                                    + previous.startTime() + "-" + previous.endTime() + " si "
                                    + current.startTime() + "-" + current.endTime());
                }
            }
        }
    }
}
