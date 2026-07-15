package com.holisun.backend.controller;

import com.holisun.backend.dto.EquipmentTypeRequest;
import com.holisun.backend.dto.EquipmentTypeResponse;
import com.holisun.backend.service.EquipmentTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * F-102 - Management Spatii si Echipamente (tipuri de echipamente medicale)
 * Endpointuri REST CRUD simplu - fara logica de business in controller
 */
@RestController
@RequestMapping("/api/equipment-types")
@RequiredArgsConstructor
public class EquipmentTypeController {

    private final EquipmentTypeService equipmentTypeService;

    @PostMapping
    public ResponseEntity<EquipmentTypeResponse> create(@Valid @RequestBody EquipmentTypeRequest dto) {
        EquipmentTypeResponse response = equipmentTypeService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EquipmentTypeResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody EquipmentTypeRequest dto
    ) {
        return ResponseEntity.ok(equipmentTypeService.update(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EquipmentTypeResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(equipmentTypeService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<EquipmentTypeResponse>> getAll() {
        return ResponseEntity.ok(equipmentTypeService.getAll());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        equipmentTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
