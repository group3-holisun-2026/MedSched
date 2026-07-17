package com.holisun.backend.service;


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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class PatientServiceImpl implements PatientService {

    @Autowired
    private PatientMapper mapper;
    @Autowired
    private PatientRepository repo;


    @Override
    public List<PatientResponse> search(String keyword) {
        List<Patient> patients;

        if (keyword == null || keyword.isBlank()) {
            patients = repo.findAll();
        } else {
            // TODO
            // If keyword is a 13-digit CNP:
            //   String hash = CnpHasher.hash(keyword);
            //   return repository.findByCnpHash(hash)...
            patients = repo.searchByNameOrPhoneOrCnp(keyword);
        }

        return patients.stream()
                .map(mapper::patientToDto)
                .toList();
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

        // TODO
        // if(dto.getCnp()!=null){
        //      String hash = CnpHasher.hash(dto.getCnp());
        //      if(patientRepository.findByCnpHash(hash).isPresent())
        //          throw new IllegalStateException(...);
        //      patient.setCnpHash(hash);
        // }


        Patient savedPatient = repo.save(patient);

        return mapper.patientToDto(savedPatient);
    }

    @Override
    public PatientResponse update(UUID id, PatientRequest dto) {
        Optional<Patient> patient = repo.findById(id);
        if (patient.isEmpty()) {
            log.error("No patient with {} id to update", id);
            throw new PatientException("No patient with that id");
        }

        mapper.updatePatientFromDto(dto, patient.get());

        // TODO
        // if(dto.getCnp()!=null){
        //      String hash = CnpHasher.hash(dto.getCnp());
        //      check duplicates...
        //      patient.setCnpHash(hash);
        // }

        Patient updatedPatient = repo.save(patient.get());

        return mapper.patientToDto(updatedPatient);

    }

    @Override
    public Page<PatientResponse> getIncomplete(String keyword, Pageable pageable) {
        Page<Patient> patients;
        if (keyword == null || keyword.isBlank()) {
            patients = repo.findByProfileCompleteFalse(pageable);
        } else {
            patients = repo.searchIncompleteByName(keyword, pageable);
        }

        return patients.map(mapper::patientToDto);
    }
}

