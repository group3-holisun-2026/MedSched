package com.holisun.backend.service;

import com.holisun.backend.repository.AppointmentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConsultationRecordLockScheduler {

    private final AppointmentRepository appointmentRepository;
    private final ConsultationRecordService consultationRecordService;

    @Scheduled(fixedRate = 60_000)
    public void lockExpiredConsultationRecords() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);

        for (UUID appointmentId :
                appointmentRepository.findAppointmentIdsReadyForRecordLock(cutoff)) {
            try {
                consultationRecordService.lock(appointmentId);
            } catch (EntityNotFoundException ignored) {
            } catch (RuntimeException exception) {
                log.warn(
                        "Nu s-a putut bloca fisa programarii {}.",
                        appointmentId,
                        exception
                );
            }
        }
    }
}