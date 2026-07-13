package backend.controller;

import backend.dto.ClinicalServiceRequest;
import backend.dto.ClinicalServiceResponse;
import backend.service.ClinicalServiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * F-103 - Catalog de Servicii.
 * Endpoint-uri REST CRUD - resursele hardware obligatorii (Equipment) sunt
 * legate prin ID-uri în {@link ClinicalServiceRequest}, rezolvarea lor
 * completă (către EquipmentResponse) se face în service, nu aici.
 */
@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ClinicalServiceController {

    private final ClinicalServiceService clinicalServiceService;

    @PostMapping
    public ResponseEntity<ClinicalServiceResponse> create(@Valid @RequestBody ClinicalServiceRequest dto) {
        ClinicalServiceResponse response = clinicalServiceService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClinicalServiceResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody ClinicalServiceRequest dto
    ) {
        return ResponseEntity.ok(clinicalServiceService.update(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClinicalServiceResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(clinicalServiceService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<ClinicalServiceResponse>> getAll() {
        return ResponseEntity.ok(clinicalServiceService.getAll());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        clinicalServiceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}