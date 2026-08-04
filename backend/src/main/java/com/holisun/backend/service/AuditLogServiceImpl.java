package com.holisun.backend.service;

import com.holisun.backend.dto.AuditLogResponse;
import com.holisun.backend.entity.AuditLog;
import com.holisun.backend.enums.AuditAction;
import com.holisun.backend.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * NFR-1 — citirea jurnalului de audit. Doar citire: {@link AuditLogRepository} e append-only
 * (expune doar {@code save} si interogarea), deci istoricul nu poate fi modificat prin API.
 *
 * Scrierea intrarilor se face din {@code AuditLoggingAspect}; aici doar le expunem.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> findByUserAndDateRange(UUID userId, LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Parametrii from si to sunt obligatorii.");
        }
        if (from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Data de inceput trebuie sa fie inaintea celei de sfarsit.");
        }

        // `userId` null inseamna "toti utilizatorii". Inainte era obligatoriu, ceea ce forta
        // interfata sa ceara un UUID inainte de a putea afisa orice — nu se putea raspunde la
        // "ce s-a intamplat ieri in clinica" fara sa stii dinainte pe cine cauti.
        List<AuditLog> entries = userId == null
                ? auditLogRepository.findByTimestampBetween(from, to)
                : auditLogRepository.findByUserIdAndTimestampBetween(userId, from, to);

        return entries.stream()
                // Cel mai recent primul: cine se uita in audit cauta aproape mereu ce s-a
                // intamplat ultima data, nu ce s-a intamplat prima data.
                .sorted(Comparator.comparing(AuditLog::getTimestamp).reversed())
                .map(this::toResponse)
                .toList();
    }

    private AuditLogResponse toResponse(AuditLog entry) {
        return new AuditLogResponse(
                entry.getId(),
                entry.getUserId(),
                parseAction(entry.getAction()),
                entry.getEntityName(),
                entry.getEntityId(),
                entry.getTimestamp()
        );
    }

    /**
     * Actiunea e stocata ca text. O valoare necunoscuta (intrare veche, rename de enum) nu are voie
     * sa arunce si sa faca tot jurnalul necitibil — o raportam si o tratam ca READ.
     */
    private AuditAction parseAction(String action) {
        try {
            return AuditAction.valueOf(action);
        } catch (IllegalArgumentException | NullPointerException e) {
            log.warn("Actiune de audit necunoscuta in baza: {}", action);
            return AuditAction.READ;
        }
    }
}
