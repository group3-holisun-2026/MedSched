package com.holisun.backend.repository;

import com.holisun.backend.entity.AuditLog;
import org.springframework.data.repository.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends Repository<AuditLog, UUID> {


    AuditLog save(AuditLog auditLog);


    List<AuditLog> findByUserIdAndTimestampBetween(UUID userId, LocalDateTime from, LocalDateTime to);
}