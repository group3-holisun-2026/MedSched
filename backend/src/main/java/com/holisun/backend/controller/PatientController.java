package com.holisun.backend.controller;

import com.holisun.backend.dto.PatientQuickCreateRequest;
import com.holisun.backend.dto.PatientRequest;
import com.holisun.backend.dto.PatientResponse;
import com.holisun.backend.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * F-201 Nomenclator Pacienti.
 * Acces: orice staff autentificat (ADMIN, DOCTOR, RECEPTION) — datele administrative din
 * Patient (nume, telefon, CNP) nu sunt supuse restrictiei clinice de la ConsultationController,
 * recepția are nevoie sa le vada/editeze ca sa programeze pacienti.
 */
@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','DOCTOR','RECEPTION')")
public class PatientController {

    private final PatientService patientService;

    @GetMapping
    public ResponseEntity<List<PatientResponse>> search(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(patientService.search(search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PatientResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(patientService.getById(id));
    }

    @PostMapping
    public ResponseEntity<PatientResponse> create(@Valid @RequestBody PatientQuickCreateRequest dto) {
        PatientResponse response = patientService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PatientResponse> update(@PathVariable UUID id, @Valid @RequestBody PatientRequest dto) {
        return ResponseEntity.ok(patientService.update(id, dto));
    }

    /**
     * Sursa de date pentru widget-ul de pe dashboard ("pacienti de completat").
     * `sort` e parametru standard Spring Data, ex. lastName,asc sau createdAt,desc.
     */
    @GetMapping("/incomplete")
    public ResponseEntity<Page<PatientResponse>> getIncomplete(
            @RequestParam(required = false) String search,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(patientService.getIncomplete(search, pageable));
    }
}
