package com.holisun.backend.service;

import com.holisun.backend.dto.ConsultationRecordRequest;
import com.holisun.backend.dto.ConsultationRecordResponse;
import com.holisun.backend.entity.Appointment;
import com.holisun.backend.entity.ConsultationRecord;
import com.holisun.backend.enums.AppointmentStatus;
import com.holisun.backend.repository.AppointmentRepository;
import com.holisun.backend.repository.ConsultationRecordRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConsultationRecordServiceImpl
        implements ConsultationRecordService {

    private final ConsultationRecordRepository consultationRecordRepository;
    private final AppointmentRepository appointmentRepository;

    @Override
    @Transactional(readOnly = true)
    public ConsultationRecordResponse getByAppointmentId(UUID appointmentId) {
        ConsultationRecord record = findRequired(appointmentId);
        return toResponse(record);
    }

    @Override
    @Transactional
    public ConsultationRecordResponse create(
            UUID appointmentId,
            ConsultationRecordRequest dto
    ) {
        assertWithinEditWindow(appointmentId);

        if (consultationRecordRepository
                .findByAppointmentId(appointmentId)
                .isPresent()) {
            throw new IllegalStateException(
                    "Exista deja o fisa de consultatie pentru programarea "
                            + appointmentId
            );
        }

        ConsultationRecord record = new ConsultationRecord();
        record.setAppointmentId(appointmentId);
        record.setLocked(false);

        applyRequest(dto, record);

        /*
         * saveAndFlush ensures that @PrePersist executes before the response
         * is constructed, so createdAt and updatedAt are available immediately.
         */
        ConsultationRecord saved =
                consultationRecordRepository.saveAndFlush(record);

        return toResponse(saved);
    }

    @Override
    @Transactional
    public ConsultationRecordResponse update(
            UUID appointmentId,
            ConsultationRecordRequest dto
    ) {
        ConsultationRecord record = findRequired(appointmentId);

        if (record.isLocked()) {
            throw new IllegalStateException(
                    "Fisa de consultatie este blocata si nu mai poate fi modificata."
            );
        }

        assertWithinEditWindow(appointmentId);

        applyRequest(dto, record);

        /*
         * Flushing triggers @PreUpdate before mapping the response, ensuring
         * that the returned updatedAt value is current.
         */
        ConsultationRecord updated =
                consultationRecordRepository.saveAndFlush(record);

        return toResponse(updated);
    }

    /**
     * Hook for Module 3/4. When an appointment reaches COMPLETED,
     * the appointment service will call this method.
     */
    @Override
    @Transactional
    public void lock(UUID appointmentId) {
        ConsultationRecord record = findRequired(appointmentId);

        if (!record.isLocked()) {
            record.setLocked(true);
            consultationRecordRepository.saveAndFlush(record);
        }
    }

    /**
     * F-402, verificat sincron la fiecare scriere — nu ne bazam doar pe flag-ul `locked` pus de
     * ConsultationRecordLockScheduler:
     *  - job-ul ruleaza o data pe minut, deci intre minutul 30 si 31 fisa ar fi ramas editabila;
     *  - job-ul nu are ce bloca daca fisa nu a fost creata deloc, asa ca fara verificarea de aici
     *    un medic putea crea fisa oricat de tarziu dupa finalizare (gaura semnalata explicit in
     *    backend_module4_tasks.md, sectiunea 1.3).
     */
    private void assertWithinEditWindow(UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Programarea " + appointmentId + " nu a fost gasita."
                ));

        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            return;
        }

        LocalDateTime completedAt = appointment.getCompletedAt();

        // completedAt lipseste doar pe programari finalizate inainte de V8__.sql; le tratam ca
        // iesite din perioada de gratie, nu ca editabile la nesfarsit.
        boolean graceExpired = completedAt == null
                || LocalDateTime.now().isAfter(completedAt.plus(EDIT_GRACE_PERIOD));

        if (graceExpired) {
            throw new IllegalStateException(
                    "Perioada de gratie de " + EDIT_GRACE_PERIOD.toMinutes()
                            + " de minute de la finalizarea consultatiei a expirat; "
                            + "fisa nu mai poate fi completata."
            );
        }
    }

    private ConsultationRecord findRequired(UUID appointmentId) {
        return consultationRecordRepository
                .findByAppointmentId(appointmentId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Fisa de consultatie pentru programarea "
                                + appointmentId
                                + " nu a fost gasita."
                ));
    }

    private void applyRequest(
            ConsultationRecordRequest dto,
            ConsultationRecord record
    ) {
        record.setPresentationMotive(dto.presentationMotive());
        record.setAnamnesis(dto.anamnesis());
        record.setClinicalExam(dto.clinicalExam());
        record.setDiagnosis(dto.diagnosis());
        record.setPrescription(dto.prescription());
    }

    private ConsultationRecordResponse toResponse(
            ConsultationRecord record
    ) {
        return new ConsultationRecordResponse(
                record.getId(),
                record.getAppointmentId(),
                record.getPresentationMotive(),
                record.getAnamnesis(),
                record.getClinicalExam(),
                record.getDiagnosis(),
                record.getPrescription(),
                record.isLocked(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }
}