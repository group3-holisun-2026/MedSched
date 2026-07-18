package com.holisun.backend.service;

import com.holisun.backend.dto.ConsultationRecordRequest;
import com.holisun.backend.dto.ConsultationRecordResponse;

import java.util.UUID;

/**
 * Contract owned by P2 (documents/module2/backend_module2_tasks.md, sectiunea 2).
 * ConsultationController (P5) depinde doar de aceasta interfata; implementarea reala
 * vine de la P2 — vezi ConsultationRecordServiceImpl.
 */
public interface ConsultationRecordService {

    ConsultationRecordResponse getByAppointmentId(UUID appointmentId);

    ConsultationRecordResponse create(UUID appointmentId, ConsultationRecordRequest dto);

    ConsultationRecordResponse update(UUID appointmentId, ConsultationRecordRequest dto);

    /**
     * Hook pentru Modulul 3/4 (tranzitia catre COMPLETED) — nu e apelat de nimeni in acest modul.
     */
    void lock(UUID appointmentId);
}
