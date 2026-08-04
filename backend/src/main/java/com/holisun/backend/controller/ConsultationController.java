package com.holisun.backend.controller;

import com.holisun.backend.aop.Audited;
import com.holisun.backend.dto.ConsultationRecordRequest;
import com.holisun.backend.dto.ConsultationRecordResponse;
import com.holisun.backend.enums.AuditAction;
import com.holisun.backend.service.ConsultationRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.holisun.backend.entity.Appointment;
import com.holisun.backend.entity.Doctor;
import com.holisun.backend.repository.AppointmentRepository;
import com.holisun.backend.repository.DoctorRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

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
    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;

    @GetMapping
    @Audited(action = AuditAction.READ, entityName = "ConsultationRecord")
    public ResponseEntity<ConsultationRecordResponse> getByAppointmentId(@PathVariable UUID appointmentId) {
        return ResponseEntity.ok(consultationRecordService.getByAppointmentId(appointmentId));
    }

    /**
     * Exportul e supus aceleiasi reguli de acces ca citirea fisei (DOCTOR/ADMIN, prin
     * @PreAuthorize pe clasa) — un PDF descarcabil de oricine ar ocoli tocmai protectia
     * continutului clinic pe care o impune F-202.
     */
    @GetMapping("/export/pdf")
    @Audited(action = AuditAction.READ, entityName = "ConsultationRecord")
    public ResponseEntity<byte[]> exportPdf(@PathVariable UUID appointmentId) {
        byte[] file = consultationRecordService.exportPdf(appointmentId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"fisa-consultatie_" + appointmentId + ".pdf\""
                )
                .body(file);
    }

    @PostMapping
    @Audited(action = AuditAction.CREATE, entityName = "ConsultationRecord")
    public ResponseEntity<ConsultationRecordResponse> create(
            @PathVariable UUID appointmentId,
            @Valid @RequestBody ConsultationRecordRequest dto
    ) {
        assertDoctorOwnsAppointment(appointmentId);
        ConsultationRecordResponse response = consultationRecordService.create(appointmentId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping
    @Audited(action = AuditAction.UPDATE, entityName = "ConsultationRecord")
    public ResponseEntity<ConsultationRecordResponse> update(
            @PathVariable UUID appointmentId,
            @Valid @RequestBody ConsultationRecordRequest dto
    ) {
        assertDoctorOwnsAppointment(appointmentId);
        return ResponseEntity.ok(consultationRecordService.update(appointmentId, dto));
    }

    private void assertDoctorOwnsAppointment(UUID appointmentId) {
        if (!hasRole("DOCTOR")) {
            return;
        }

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Programarea nu a fost gasita."
                ));

        Doctor currentDoctor = doctorRepository.findByUserId(currentUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Contul curent nu este asociat unui medic."
                ));

        if (!currentDoctor.getId().equals(appointment.getDoctor().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Doar medicul alocat poate modifica fisa de consultatie."
            );
        }
    }

    private UUID currentUserId() {
        return (UUID) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }

    private boolean hasRole(String role) {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_" + role));
    }


}
