package com.holisun.backend.repository;

import com.holisun.backend.entity.AuditLog;
import org.springframework.data.repository.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends Repository<AuditLog, UUID> {


    AuditLog save(AuditLog auditLog);


    List<AuditLog> findByUserIdAndTimestampBetween(UUID userId, LocalDateTime from, LocalDateTime to);

    /** Varianta fara filtru de utilizator: "ce s-a intamplat in clinica in intervalul asta". */
    List<AuditLog> findByTimestampBetween(LocalDateTime from, LocalDateTime to);
}