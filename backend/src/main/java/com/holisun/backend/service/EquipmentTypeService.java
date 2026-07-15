package com.holisun.backend.service;

import com.holisun.backend.dto.EquipmentTypeRequest;
import com.holisun.backend.dto.EquipmentTypeResponse;
import com.holisun.backend.entity.EquipmentType;
import com.holisun.backend.mapper.EquipmentMapper;
import com.holisun.backend.repository.EquipmentTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EquipmentTypeService {

    private final EquipmentTypeRepository equipmentTypeRepository;
    private final EquipmentMapper equipmentMapper;

    public EquipmentTypeResponse create(EquipmentTypeRequest dto) {
        if (equipmentTypeRepository.existsByName(dto.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Equipment type name already exists");
        }

        EquipmentType equipmentType = new EquipmentType();
        equipmentType.setName(dto.getName());
        equipmentType.setDescription(dto.getDescription());

        EquipmentType saved = equipmentTypeRepository.save(equipmentType);
        return equipmentMapper.toResponse(saved);
    }

    public EquipmentTypeResponse update(UUID id, EquipmentTypeRequest dto) {
        EquipmentType equipmentType = equipmentTypeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipment type not found"));

        equipmentType.setName(dto.getName());
        equipmentType.setDescription(dto.getDescription());

        EquipmentType saved = equipmentTypeRepository.save(equipmentType);
        return equipmentMapper.toResponse(saved);
    }

    public EquipmentTypeResponse getById(UUID id) {
        EquipmentType equipmentType = equipmentTypeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipment type not found"));
        return equipmentMapper.toResponse(equipmentType);
    }

    public List<EquipmentTypeResponse> getAll() {
        return equipmentTypeRepository.findAll()
                .stream()
                .map(equipmentMapper::toResponse)
                .toList();
    }

    public void delete(UUID id) {
        if (!equipmentTypeRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipment type not found");
        }
        equipmentTypeRepository.deleteById(id);
    }
}
