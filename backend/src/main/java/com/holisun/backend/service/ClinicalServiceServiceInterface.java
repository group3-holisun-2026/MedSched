package com.holisun.backend.service;

import com.holisun.backend.dto.ClinicalServiceRequest;
import com.holisun.backend.dto.ClinicalServiceResponse;

import java.util.List;
import java.util.UUID;

public interface ClinicalServiceServiceInterface {
    ClinicalServiceResponse create(ClinicalServiceRequest dto);
    ClinicalServiceResponse update(UUID id, ClinicalServiceRequest dto);
    ClinicalServiceResponse getById(UUID id);
    List<ClinicalServiceResponse> getAll();
    void delete(UUID id);
    List<ClinicalServiceResponse> findByName(String name); //for future use, if needed
}
