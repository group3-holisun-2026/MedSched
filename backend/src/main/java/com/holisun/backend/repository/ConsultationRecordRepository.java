package com.holisun.backend.repository;

import com.holisun.backend.entity.ConsultationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConsultationRecordRepository
        extends JpaRepository<ConsultationRecord, UUID> {

    Optional<ConsultationRecord> findByAppointmentId(UUID appointmentId);

    /** Incarcarea in bloc a fiselor unui pacient — altfel lista lui ar face cate o cerere pe rand. */
    List<ConsultationRecord> findByAppointmentIdIn(Collection<UUID> appointmentIds);
}