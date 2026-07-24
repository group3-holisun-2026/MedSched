package com.holisun.backend.controller;

import com.holisun.backend.dto.AppointmentRequest;
import com.holisun.backend.dto.AppointmentResponse;
import com.holisun.backend.dto.CalendarAppointmentResponse;
import com.holisun.backend.entity.Doctor;
import com.holisun.backend.repository.DoctorRepository;
import com.holisun.backend.service.AppointmentService;
import com.holisun.backend.service.CalendarService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * F-301/F-302 — Programari + Calendar.
 * Creare/reprogramare/anulare: doar ADMIN/RECEPTION (medicul nu isi creeaza singur programari).
 * Citire: ADMIN, DOCTOR, RECEPTION — dar DOCTOR e fortat pe propriul calendar (nu poate cere
 * calendarul altui medic doar schimband doctorIds din query).
 */

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final CalendarService calendarService;
    private final DoctorRepository doctorRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION')")
    public ResponseEntity<AppointmentResponse> create(@Valid @RequestBody AppointmentRequest dto) {
        AppointmentResponse response = appointmentService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION')")
    public ResponseEntity<AppointmentResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody AppointmentRequest dto
    ) {
        return ResponseEntity.ok(appointmentService.update(id, dto));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION')")
    public ResponseEntity<AppointmentResponse> cancel(@PathVariable UUID id) {
        appointmentService.cancel(id);
        return ResponseEntity.ok(appointmentService.getById(id));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','RECEPTION')")
    public ResponseEntity<AppointmentResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(appointmentService.getById(id));
    }

    @GetMapping("/calendar")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','RECEPTION')")
    public ResponseEntity<List<CalendarAppointmentResponse>> getCalendar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) List<UUID> doctorIds,
            @RequestParam(required = false) UUID roomId
    ) {
        if (hasRole("DOCTOR")) {
            Doctor doctor = doctorRepository.findByUserId(currentUserId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Contul curent nu este asociat unui medic"));
            return ResponseEntity.ok(calendarService.getForDoctor(doctor.getId(), from, to));
        }

        if (roomId != null) {
            return ResponseEntity.ok(calendarService.getByDateRangeAndRoom(from, to, roomId));
        }

        return ResponseEntity.ok(calendarService.getByDateRangeAndDoctors(from, to, doctorIds));
    }

    @PatchMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTION')")
    public ResponseEntity<AppointmentResponse> confirm(@PathVariable UUID id) {
        appointmentService.confirm(id);
        return ResponseEntity.ok(appointmentService.getById(id));
    }

    @PatchMapping("/{id}/check-in")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTION')")
    public ResponseEntity<AppointmentResponse> checkIn(@PathVariable UUID id) {
        boolean isDoctorRole = hasRole("DOCTOR");
        UUID callerUserId = isDoctorRole ? currentUserId() : null;
        appointmentService.checkIn(id, callerUserId, isDoctorRole);
        return ResponseEntity.ok(appointmentService.getById(id));

    }

    @PatchMapping("/{id}/no-show")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION')")
    public ResponseEntity<AppointmentResponse> noShow(@PathVariable UUID id) {
        appointmentService.noShow(id);
        return ResponseEntity.ok(appointmentService.getById(id));
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")
    public ResponseEntity<AppointmentResponse> complete(@PathVariable UUID id) {

        // TODO:
        // P1's implementation of complete doesn't allow a null id (which would be for the ADMIN role)
        // either beg person1 to change his or ...

        UUID doctorId = null;

        if (hasRole("DOCTOR")) {
            Optional<Doctor> doctor = doctorRepository.findByUserId(currentUserId());
            if (doctor.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contul curent nu este medic");
            }
            doctorId = doctor.get().getId();
        }
        appointmentService.complete(id, doctorId);
        return ResponseEntity.ok(appointmentService.getById(id));
    }


    private UUID currentUserId() {
        return (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private boolean hasRole(String role) {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_" + role));
    }
}
