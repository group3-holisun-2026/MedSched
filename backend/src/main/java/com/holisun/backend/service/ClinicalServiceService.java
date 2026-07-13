package com.holisun.backend.service;

import com.holisun.backend.dto.ClinicalServiceRequest;
import com.holisun.backend.dto.ClinicalServiceResponse;
import com.holisun.backend.dto.EquipmentResponse;
import com.holisun.backend.entity.ClinicalService;
import com.holisun.backend.entity.Equipment;
import com.holisun.backend.repository.ClinicalServiceRepository;
import com.holisun.backend.repository.EquipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClinicalServiceService {

    private final ClinicalServiceRepository clinicalServiceRepository;
    private final EquipmentRepository equipmentRepository;

    public ClinicalServiceResponse create(ClinicalServiceRequest dto) {
        Set<Equipment> equipment = resolveEquipment(dto.getRequiredEquipmentIds());

        ClinicalService service = new ClinicalService();
        service.setName(dto.getName());
        service.setPrice(dto.getPrice());
        service.setDefaultDurationMinutes(dto.getDefaultDurationMinutes());
        service.setRequiredEquipment(equipment);

        ClinicalService saved = clinicalServiceRepository.save(service);
        return toResponse(saved);
    }

    public ClinicalServiceResponse update(UUID id, ClinicalServiceRequest dto) {
        ClinicalService service = clinicalServiceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clinical service not found"));

        Set<Equipment> equipment = resolveEquipment(dto.getRequiredEquipmentIds());

        service.setName(dto.getName());
        service.setPrice(dto.getPrice());
        service.setDefaultDurationMinutes(dto.getDefaultDurationMinutes());
        service.setRequiredEquipment(equipment);

        ClinicalService saved = clinicalServiceRepository.save(service);
        return toResponse(saved);
    }

    public ClinicalServiceResponse getById(UUID id) {
        ClinicalService service = clinicalServiceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clinical service not found"));
        return toResponse(service);
    }

    public List<ClinicalServiceResponse> getAll() {
        return clinicalServiceRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public void delete(UUID id) {
        if (!clinicalServiceRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Clinical service not found");
        }
        clinicalServiceRepository.deleteById(id);
    }

    private Set<Equipment> resolveEquipment(List<UUID> ids) {
        List<Equipment> found = equipmentRepository.findByIdIn(ids);
        if (found.size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "One or more equipment IDs are invalid");
        }
        return new HashSet<>(found);
    }

    private ClinicalServiceResponse toResponse(ClinicalService service) {
        List<EquipmentResponse> equipmentResponses = service.getRequiredEquipment()
                .stream()
                .map(e -> new EquipmentResponse(e.getId(), e.getName(), e.isActive()))
                .toList();

        return new ClinicalServiceResponse(
                service.getId(),
                service.getName(),
                service.getPrice(),
                service.getDefaultDurationMinutes(),
                equipmentResponses
        );
    }
}