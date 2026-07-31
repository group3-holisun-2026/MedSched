package com.holisun.backend.service;

import com.holisun.backend.repository.AppointmentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultationRecordLockSchedulerTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private ConsultationRecordService consultationRecordService;

    @InjectMocks
    private ConsultationRecordLockScheduler scheduler;

    @Test
    void locksOnlyAppointmentsReturnedAsReady() {
        UUID expiredAppointmentId = UUID.randomUUID();
        UUID recentAppointmentId = UUID.randomUUID();

        when(appointmentRepository
                .findAppointmentIdsReadyForRecordLock(any(LocalDateTime.class)))
                .thenReturn(List.of(expiredAppointmentId));

        scheduler.lockExpiredConsultationRecords();

        verify(consultationRecordService).lock(expiredAppointmentId);
        verify(consultationRecordService, never()).lock(recentAppointmentId);
    }

    @Test
    void missingRecordDoesNotStopTheRemainingBatch() {
        UUID missingRecordId = UUID.randomUUID();
        UUID validRecordId = UUID.randomUUID();

        when(appointmentRepository
                .findAppointmentIdsReadyForRecordLock(any(LocalDateTime.class)))
                .thenReturn(List.of(missingRecordId, validRecordId));

        doThrow(new EntityNotFoundException())
                .when(consultationRecordService)
                .lock(missingRecordId);

        scheduler.lockExpiredConsultationRecords();

        verify(consultationRecordService).lock(missingRecordId);
        verify(consultationRecordService).lock(validRecordId);
    }
}