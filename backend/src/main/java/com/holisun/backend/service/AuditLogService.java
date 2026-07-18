package com.holisun.backend.service;

import com.holisun.backend.dto.AuditLogResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Contract owned by P3 (documents/module2/backend_module2_tasks.md, sectiunea 3) — subtire
 * wrapper peste AuditLogRepository.findByUserAndDateRange, folosit de AuditController.
 */
public interface AuditLogService {

    List<AuditLogResponse> findByUserAndDateRange(UUID userId, LocalDateTime from, LocalDateTime to);
}
