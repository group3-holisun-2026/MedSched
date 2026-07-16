package com.holisun.backend.dto;

import com.holisun.backend.enums.AuditAction;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Placeholder — owner real: P3 (documents/module2/backend_module2_tasks.md, sectiunea 3).
 */
public record AuditLogResponse(
        UUID id,
        UUID userId,
        AuditAction action,
        String entityName,
        UUID entityId,
        LocalDateTime timestamp
) {
}
