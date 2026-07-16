package com.holisun.backend.service;

import com.holisun.backend.dto.AuditLogResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * TODO(P3): inlocuieste cu implementarea reala (AuditLogRepository.findByUserAndDateRange),
 * vezi documents/module2/backend_module2_tasks.md sectiunea 3. Placeholder doar ca sa
 * porneasca aplicatia cat timp infrastructura de audit nu e implementata.
 */
@Service
public class AuditLogServiceImpl implements AuditLogService {

    @Override
    public List<AuditLogResponse> findByUserAndDateRange(UUID userId, LocalDateTime from, LocalDateTime to) {
        throw new UnsupportedOperationException("AuditLogService.findByUserAndDateRange - nu e inca implementat de P3");
    }
}
