package com.holisun.backend.service;

import com.holisun.backend.dto.ClinicalServiceRequest;
import com.holisun.backend.dto.ClinicalServiceResponse;
import com.holisun.backend.entity.EquipmentType;
import com.holisun.backend.exception.ClinicalServiceException;
import com.holisun.backend.mapper.ClinicalServiceMapper;
import com.holisun.backend.repository.ClinicalServiceRepository;
import com.holisun.backend.repository.EquipmentTypeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class ClinicalServiceService implements ClinicalServiceServiceInterface {
    @Autowired
    private ClinicalServiceMapper clinicalServiceMapper;
    @Autowired
    private ClinicalServiceRepository clinicalServiceRepository;
    @Autowired
    private EquipmentTypeRepository equipmentTypeRepository;

    @Override
    public ClinicalServiceResponse create(ClinicalServiceRequest dto) {
        List<EquipmentType> equipmentTypes = equipmentTypeRepository.findAllById(dto.getRequiredEquipmentTypeIds());
        if (equipmentTypes.size() != dto.getRequiredEquipmentTypeIds().size()) {
            log.error("One or more equipment type IDs are invalid");
            throw new ClinicalServiceException("One or more equipment type IDs are invalid");
        }
        com.holisun.backend.entity.Service clinicalService = clinicalServiceMapper.dtoToClinicalService(dto);
        clinicalService.setRequiredEquipmentTypes(new HashSet<>(equipmentTypes));

        clinicalService.setActive(dto.getActive() != null ? dto.getActive() : true);

        com.holisun.backend.entity.Service saved = clinicalServiceRepository.save(clinicalService);
        return clinicalServiceMapper.clinicalServiceToDto(saved);
    }

    @Override
    public ClinicalServiceResponse update(UUID id, ClinicalServiceRequest dto) {
        com.holisun.backend.entity.Service existingService = clinicalServiceRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Service not found for update: {}", id);
                    return new ClinicalServiceException("Service not found: " + id);
                });

        existingService.setName(dto.getName());
        existingService.setPrice(dto.getPrice());
        existingService.setDefaultDurationMinutes(dto.getDefaultDurationMinutes());

        if (dto.getActive() != null) {
            existingService.setActive(dto.getActive());
        }

        List<EquipmentType> equipmentTypes = equipmentTypeRepository.findAllById(dto.getRequiredEquipmentTypeIds());
        if (equipmentTypes.size() != dto.getRequiredEquipmentTypeIds().size()) {
            log.error("Invalid equipment type IDs during update for service {}", id);
            throw new ClinicalServiceException("One or more equipment type IDs are invalid");
        }
        existingService.setRequiredEquipmentTypes(new HashSet<>(equipmentTypes));

        com.holisun.backend.entity.Service saved = clinicalServiceRepository.save(existingService);
        return clinicalServiceMapper.clinicalServiceToDto(saved);
    }


    @Override
    public List<ClinicalServiceResponse> findByName(String name) {
        List<com.holisun.backend.entity.Service> services = clinicalServiceRepository.findByName(name);
        List<ClinicalServiceResponse> clinicalServiceResponses = new ArrayList<>();
        for (com.holisun.backend.entity.Service service : services) {
            clinicalServiceResponses.add(clinicalServiceMapper.clinicalServiceToDto(service));
        }
        return clinicalServiceResponses;
    }

    @Override
    public ClinicalServiceResponse getById(UUID id) {
        Optional<com.holisun.backend.entity.Service> clinicalService = clinicalServiceRepository.findById(id);
        if (clinicalService.isEmpty()) {
            log.error("Not clinical service with id: {}", id);
            throw new ClinicalServiceException("Service not found: " + id);

        }
        return clinicalServiceMapper.clinicalServiceToDto(clinicalService.get());
    }


    @Override
    public List<ClinicalServiceResponse> getAll() {
        List<com.holisun.backend.entity.Service> clinicalServices = clinicalServiceRepository.findAll();
        List<ClinicalServiceResponse> clinicalServiceResponses = new ArrayList<>();
        for (com.holisun.backend.entity.Service service : clinicalServices) {
            clinicalServiceResponses.add(clinicalServiceMapper.clinicalServiceToDto(service));
        }
        return clinicalServiceResponses;
    }

    @Override
    public void delete(UUID id) {
        if (!clinicalServiceRepository.existsById(id)) {
            log.error("Service not found for delete: {}", id);
            throw new ClinicalServiceException("Service not found: " + id);
        }
        clinicalServiceRepository.deleteById(id);
        log.info("Deleted clinical service {}", id);
    }


}
