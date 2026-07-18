package com.holisun.backend.controller;

import com.holisun.backend.aop.Audited;
import com.holisun.backend.dto.ConsultationRecordRequest;
import com.holisun.backend.dto.ConsultationRecordResponse;
import com.holisun.backend.enums.AuditAction;
import com.holisun.backend.service.ConsultationRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * F-202 Fisa de Consultatie.
 * Cerinta de securitate: continutul e protejat impotriva vizualizarii neautorizate de
 * utilizatori fara drepturi clinice — RECEPTION exclus explicit, doar DOCTOR/ADMIN.
 */
@RestController
@RequestMapping("/api/appointments/{appointmentId}/record")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
public class ConsultationController {

    private final ConsultationRecordService consultationRecordService;

    @GetMapping
    @Audited(action = AuditAction.READ, entityName = "ConsultationRecord")
    public ResponseEntity<ConsultationRecordResponse> getByAppointmentId(@PathVariable UUID appointmentId) {
        return ResponseEntity.ok(consultationRecordService.getByAppointmentId(appointmentId));
    }

    @PostMapping
    @Audited(action = AuditAction.CREATE, entityName = "ConsultationRecord")
    public ResponseEntity<ConsultationRecordResponse> create(
            @PathVariable UUID appointmentId,
            @Valid @RequestBody ConsultationRecordRequest dto
    ) {
        ConsultationRecordResponse response = consultationRecordService.create(appointmentId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping
    @Audited(action = AuditAction.UPDATE, entityName = "ConsultationRecord")
    public ResponseEntity<ConsultationRecordResponse> update(
            @PathVariable UUID appointmentId,
            @Valid @RequestBody ConsultationRecordRequest dto
    ) {
        return ResponseEntity.ok(consultationRecordService.update(appointmentId, dto));
    }
}
