package com.holisun.backend.service;

import com.holisun.backend.dto.PatientQuickCreateRequest;
import com.holisun.backend.dto.PatientRequest;
import com.holisun.backend.dto.PatientResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * TODO(P1): inlocuieste cu implementarea reala (entity/repository/business logic),
 * vezi documents/module2/backend_module2_tasks.md sectiunea 1. Placeholder doar
 * ca sa porneasca aplicatia cat timp domeniul Patient nu e implementat.
 */
@Service
public class PatientServiceImpl implements PatientService {

    @Override
    public List<PatientResponse> search(String keyword) {
        throw new UnsupportedOperationException("PatientService.search - nu e inca implementat de P1");
    }

    @Override
    public PatientResponse getById(UUID id) {
        throw new UnsupportedOperationException("PatientService.getById - nu e inca implementat de P1");
    }

    @Override
    public PatientResponse create(PatientQuickCreateRequest dto) {
        throw new UnsupportedOperationException("PatientService.create - nu e inca implementat de P1");
    }

    @Override
    public PatientResponse update(UUID id, PatientRequest dto) {
        throw new UnsupportedOperationException("PatientService.update - nu e inca implementat de P1");
    }

    @Override
    public Page<PatientResponse> getIncomplete(String keyword, Pageable pageable) {
        throw new UnsupportedOperationException("PatientService.getIncomplete - nu e inca implementat de P1");
    }
}
