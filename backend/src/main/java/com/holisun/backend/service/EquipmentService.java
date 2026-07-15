package com.holisun.backend.service;

import com.holisun.backend.dto.EquipmentRequest;
import com.holisun.backend.dto.EquipmentResponse;
import com.holisun.backend.entity.Equipment;
import com.holisun.backend.entity.EquipmentType;
import com.holisun.backend.entity.Room;
import com.holisun.backend.mapper.EquipmentMapper;
import com.holisun.backend.repository.EquipmentRepository;
import com.holisun.backend.repository.EquipmentTypeRepository;
import com.holisun.backend.repository.RoomRepository;
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
    private final EquipmentTypeRepository equipmentTypeRepository;
    private final RoomRepository roomRepository;
    private final EquipmentMapper equipmentMapper;

    public EquipmentResponse create(EquipmentRequest dto) {
        if (equipmentRepository.existsByName(dto.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Equipment name already exists");
        }

        EquipmentType equipmentType = equipmentTypeRepository.findById(dto.getEquipmentTypeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipment type not found"));

        Equipment equipment = new Equipment();
        equipment.setName(dto.getName());
        equipment.setEquipmentType(equipmentType);
        equipment.setRoom(resolveRoom(dto.getRoomId()));
        equipment.setActive(true);

        Equipment saved = equipmentRepository.save(equipment);
        return equipmentMapper.toResponse(saved);
    }

    public EquipmentResponse update(UUID id, EquipmentRequest dto) {
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipment not found"));

        EquipmentType equipmentType = equipmentTypeRepository.findById(dto.getEquipmentTypeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipment type not found"));

        equipment.setName(dto.getName());
        equipment.setEquipmentType(equipmentType);
        equipment.setRoom(resolveRoom(dto.getRoomId()));

        Equipment saved = equipmentRepository.save(equipment);
        return equipmentMapper.toResponse(saved);
    }

    public EquipmentResponse getById(UUID id) {
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipment not found"));
        return equipmentMapper.toResponse(equipment);
    }

    public List<EquipmentResponse> getAll() {
        return equipmentRepository.findAll()
                .stream()
                .map(equipmentMapper::toResponse)
                .toList();
    }

    public void delete(UUID id) {
        if (!equipmentRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipment not found");
        }
        equipmentRepository.deleteById(id);
    }

    private Room resolveRoom(UUID roomId) {
        if (roomId == null) {
            return null;
        }
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
    }
}