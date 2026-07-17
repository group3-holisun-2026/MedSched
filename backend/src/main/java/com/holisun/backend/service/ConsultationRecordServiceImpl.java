package com.holisun.backend.service;

import com.holisun.backend.dto.ConsultationRecordRequest;
import com.holisun.backend.dto.ConsultationRecordResponse;
import com.holisun.backend.entity.ConsultationRecord;
import com.holisun.backend.repository.ConsultationRecordRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConsultationRecordServiceImpl
        implements ConsultationRecordService {

    private final ConsultationRecordRepository consultationRecordRepository;

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