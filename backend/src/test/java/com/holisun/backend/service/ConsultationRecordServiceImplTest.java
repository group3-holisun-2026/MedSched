package com.holisun.backend.service;

import com.holisun.backend.dto.ConsultationRecordRequest;
import com.holisun.backend.dto.ConsultationRecordResponse;
import com.holisun.backend.entity.ConsultationRecord;
import com.holisun.backend.repository.ConsultationRecordRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsultationRecordServiceImplTest {

    @Mock
    private ConsultationRecordRepository consultationRecordRepository;

    @InjectMocks
    private ConsultationRecordServiceImpl consultationRecordService;

    @Test
    void getByAppointmentIdThrowsNotFoundWhenRecordDoesNotExist() {
        UUID appointmentId = UUID.randomUUID();

        when(consultationRecordRepository.findByAppointmentId(appointmentId))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> consultationRecordService.getByAppointmentId(appointmentId)
        );
    }

    @Test
    void createThrowsConflictWhenRecordAlreadyExists() {
        UUID appointmentId = UUID.randomUUID();
        ConsultationRecord existing = createExistingRecord(
                appointmentId,
                false
        );

        when(consultationRecordRepository.findByAppointmentId(appointmentId))
                .thenReturn(Optional.of(existing));

        assertThrows(
                IllegalStateException.class,
                () -> consultationRecordService.create(
                        appointmentId,
                        createRequest()
                )
        );

        verify(consultationRecordRepository, never())
                .saveAndFlush(any());
    }

    @Test
    void createSavesAndReturnsNewRecord() {
        UUID appointmentId = UUID.randomUUID();

        when(consultationRecordRepository.findByAppointmentId(appointmentId))
                .thenReturn(Optional.empty());

        when(consultationRecordRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> {
                    ConsultationRecord saved = invocation.getArgument(0);
                    LocalDateTime now = LocalDateTime.now();

                    saved.setId(UUID.randomUUID());
                    saved.setCreatedAt(now);
                    saved.setUpdatedAt(now);

                    return saved;
                });

        ConsultationRecordResponse response =
                consultationRecordService.create(
                        appointmentId,
                        createRequest()
                );

        assertNotNull(response.id());
        assertEquals(appointmentId, response.appointmentId());
        assertEquals(
                "Durere de cap",
                response.presentationMotive()
        );
        assertEquals("Migrena", response.diagnosis());
        assertFalse(response.locked());
        assertNotNull(response.createdAt());
        assertNotNull(response.updatedAt());

        verify(consultationRecordRepository)
                .saveAndFlush(any());
    }

    @Test
    void updateThrowsConflictWhenRecordIsLocked() {
        UUID appointmentId = UUID.randomUUID();

        ConsultationRecord lockedRecord = createExistingRecord(
                appointmentId,
                true
        );

        when(consultationRecordRepository.findByAppointmentId(appointmentId))
                .thenReturn(Optional.of(lockedRecord));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> consultationRecordService.update(
                        appointmentId,
                        createRequest()
                )
        );

        assertTrue(exception.getMessage().contains("blocata"));

        verify(consultationRecordRepository, never())
                .saveAndFlush(any());
    }

    @Test
    void updateChangesUnlockedRecord() {
        UUID appointmentId = UUID.randomUUID();

        ConsultationRecord existing = createExistingRecord(
                appointmentId,
                false
        );

        when(consultationRecordRepository.findByAppointmentId(appointmentId))
                .thenReturn(Optional.of(existing));

        when(consultationRecordRepository.saveAndFlush(existing))
                .thenReturn(existing);

        ConsultationRecordResponse response =
                consultationRecordService.update(
                        appointmentId,
                        createRequest()
                );

        assertEquals(
                "Durere de cap",
                response.presentationMotive()
        );
        assertEquals(
                "Pacientul are simptome de doua zile",
                response.anamnesis()
        );
        assertEquals("Migrena", response.diagnosis());
        assertEquals(
                "Tratament recomandat",
                response.prescription()
        );
        assertFalse(response.locked());

        verify(consultationRecordRepository)
                .saveAndFlush(existing);
    }

    @Test
    void lockMarksRecordAsLocked() {
        UUID appointmentId = UUID.randomUUID();

        ConsultationRecord existing = createExistingRecord(
                appointmentId,
                false
        );

        when(consultationRecordRepository.findByAppointmentId(appointmentId))
                .thenReturn(Optional.of(existing));

        when(consultationRecordRepository.saveAndFlush(existing))
                .thenReturn(existing);

        consultationRecordService.lock(appointmentId);

        assertTrue(existing.isLocked());

        verify(consultationRecordRepository)
                .saveAndFlush(existing);
    }

    private ConsultationRecordRequest createRequest() {
        return new ConsultationRecordRequest(
                "Durere de cap",
                "Pacientul are simptome de doua zile",
                "Examen clinic normal",
                "Migrena",
                "Tratament recomandat"
        );
    }

    private ConsultationRecord createExistingRecord(
            UUID appointmentId,
            boolean locked
    ) {
        LocalDateTime now = LocalDateTime.now();

        ConsultationRecord record = new ConsultationRecord();
        record.setId(UUID.randomUUID());
        record.setAppointmentId(appointmentId);
        record.setPresentationMotive("Motiv initial");
        record.setAnamnesis("Anamneza initiala");
        record.setClinicalExam("Examen initial");
        record.setDiagnosis("Diagnostic initial");
        record.setPrescription("Prescriptie initiala");
        record.setLocked(locked);
        record.setCreatedAt(now);
        record.setUpdatedAt(now);

        return record;
    }
}