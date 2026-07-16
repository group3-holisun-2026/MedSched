package com.holisun.backend.service;

import com.holisun.backend.dto.PatientQuickCreateRequest;
import com.holisun.backend.dto.PatientRequest;
import com.holisun.backend.dto.PatientResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Contract owned by P1 (documents/module2/backend_module2_tasks.md, sectiunea 1).
 * PatientController (P5) depinde doar de aceasta interfata; implementarea reala
 * (entity/repository/logica de business) vine de la P1 — vezi PatientServiceImpl.
 */
public interface PatientService {

    List<PatientResponse> search(String keyword);

    PatientResponse getById(UUID id);

    PatientResponse create(PatientQuickCreateRequest dto);

    PatientResponse update(UUID id, PatientRequest dto);

    Page<PatientResponse> getIncomplete(String keyword, Pageable pageable);
}
