package com.holisun.backend.service;

import com.holisun.backend.dto.EquipmentRequest;
import com.holisun.backend.dto.EquipmentResponse;
import com.holisun.backend.entity.Equipment;
import com.holisun.backend.repository.EquipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;

    public EquipmentResponse create(EquipmentRequest dto) {
        if (equipmentRepository.existsByName(dto.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Equipment name already exists");
        }

        Equipment equipment = new Equipment();
        equipment.setName(dto.getName());
        equipment.setActive(true);

        Equipment saved = equipmentRepository.save(equipment);
        return toResponse(saved);
    }

    public EquipmentResponse update(UUID id, EquipmentRequest dto) {
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipment not found"));

        equipment.setName(dto.getName());

        Equipment saved = equipmentRepository.save(equipment);
        return toResponse(saved);
    }

    public EquipmentResponse getById(UUID id) {
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipment not found"));
        return toResponse(equipment);
    }

    public List<EquipmentResponse> getAll() {
        return equipmentRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public void delete(UUID id) {
        if (!equipmentRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipment not found");
        }
        equipmentRepository.deleteById(id);
    }

    private EquipmentResponse toResponse(Equipment equipment) {
        return new EquipmentResponse(
                equipment.getId(),
                equipment.getName(),
                equipment.isActive()
        );
    }
}