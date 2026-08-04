package com.holisun.backend.service;

import com.holisun.backend.dto.ConsultationRecordRequest;
import com.holisun.backend.dto.ConsultationRecordResponse;

import java.time.Duration;
import java.util.UUID;

/**
 * Contract owned by P2 (documents/module2/backend_module2_tasks.md, sectiunea 2).
 * ConsultationController (P5) depinde doar de aceasta interfata; implementarea reala
 * vine de la P2 — vezi ConsultationRecordServiceImpl.
 */
public interface ConsultationRecordService {

    /**
     * F-402: dupa trecerea programarii in COMPLETED, fisa mai poate fi completata/corectata
     * exact atat timp. Constanta e folosita si de serviciu (respingere sincrona la scriere),
     * si de ConsultationRecordLockScheduler (marcarea fisei ca `locked`), ca cele doua sa nu
     * poata ajunge la valori diferite.
     */
    Duration EDIT_GRACE_PERIOD = Duration.ofMinutes(30);

    ConsultationRecordResponse getByAppointmentId(UUID appointmentId);

    ConsultationRecordResponse create(UUID appointmentId, ConsultationRecordRequest dto);

    ConsultationRecordResponse update(UUID appointmentId, ConsultationRecordRequest dto);

    /**
     * Fisa in format PDF, pentru dosarul pacientului. Exportul e o citire: nu tine cont de
     * fereastra de editare — o fisa blocata trebuie sa ramana tiparibila.
     */
    byte[] exportPdf(UUID appointmentId);

    /**
     * Hook pentru Modulul 3/4 (tranzitia catre COMPLETED) — nu e apelat de nimeni in acest modul.
     */
    void lock(UUID appointmentId);
}
