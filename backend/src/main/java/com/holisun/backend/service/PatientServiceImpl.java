package com.holisun.backend.service;

import com.holisun.backend.util.CnpHasher;
import com.holisun.backend.dto.PatientQuickCreateRequest;
import com.holisun.backend.dto.PatientRequest;
import com.holisun.backend.dto.PatientResponse;
import com.holisun.backend.entity.Patient;
import com.holisun.backend.exception.PatientException;
import com.holisun.backend.mapper.PatientMapper;
import com.holisun.backend.repository.PatientRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class PatientServiceImpl implements PatientService {

    @Autowired
    private PatientMapper mapper;
    @Autowired
    private PatientRepository repo;


    @Override
    public List<PatientResponse> search(String search) {

        if (isCnp(search)) {

            String hash = CnpHasher.hash(search);

            Optional<Patient> patient = repo.findByCnpHash(hash);
            if (patient.isEmpty()) {
                log.warn("No patient with that cnp: {}", search);

            }
            return Collections.singletonList(mapper.patientToDto(patient.get()));

        }

        if (isPhone(search)) {
            List<Patient> patients = repo.findByPhoneContaining(search);
            List<PatientResponse> responses = new ArrayList<>();

            for (Patient patient : patients) {
                responses.add(mapper.patientToDto(patient));
            }

            return responses;

        }


        List<Patient> patients = repo.searchByName(search);
        List<PatientResponse> responses = new ArrayList<>();

        for (Patient patient : patients) {
            responses.add(mapper.patientToDto(patient));
        }

        return responses;
    }

    private boolean isPhone(String value) {
        return value.matches("\\d{7,15}");
    }

    private boolean isCnp(String value) {
        return value.matches("\\d{13}");
    }


    @Override
    public PatientResponse getById(UUID id) {
        Optional<Patient> patient = repo.findById(id);
        if (patient.isEmpty()) {
            log.error("Not patient with id: {}", id);
            throw new PatientException("No patient with that id");
        }
        return mapper.patientToDto(patient.get());
    }

    @Override
    public PatientResponse create(PatientQuickCreateRequest dto) {
        Patient patient = mapper.dtoToPatient(dto);

        if (patient.getCnp() != null && !patient.getCnp().isBlank()) {

            String hash = CnpHasher.hash(patient.getCnp());

            if (repo.findByCnpHash(hash).isPresent()) {
                throw new IllegalStateException("A patient with this CNP already exists.");
            }

            patient.setCnpHash(hash);
        }

        Patient saved = repo.save(patient);

        return mapper.patientToDto(saved);
    }

    @Override
    public PatientResponse update(UUID id, PatientRequest dto) {
        Patient patient = repo.findById(id)
                .orElseThrow(() ->
                        new PatientException("Patient not found: " + id));

        mapper.updatePatientFromDto(dto, patient);

        if (patient.getCnp() != null && !patient.getCnp().isBlank()) {

            String hash = CnpHasher.hash(patient.getCnp());

            repo.findByCnpHash(hash)
                    .ifPresent(existing -> {
                        if (!existing.getId().equals(patient.getId())) {
                            throw new IllegalStateException("A patient with this CNP already exists.");
                        }
                    });

            patient.setCnpHash(hash);
        } else {
            patient.setCnpHash(null);
        }

        Patient updated = repo.save(patient);

        return mapper.patientToDto(updated);

    }

    @Override
    public Page<PatientResponse> getIncomplete(String keyword, Pageable pageable) {
        Page<Patient> page;

        if (keyword == null || keyword.isBlank()) {
            page = repo.findByProfileCompleteFalse(pageable);
        } else {
            page = repo.searchIncompleteByName(keyword, pageable);
        }

        return page.map(mapper::patientToDto);
    }
}

