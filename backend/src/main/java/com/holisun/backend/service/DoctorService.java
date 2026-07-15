package com.holisun.backend.service;

import com.holisun.backend.dto.DoctorCreateRequest;
import com.holisun.backend.dto.DoctorResponse;
import com.holisun.backend.dto.DoctorUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface DoctorService {
    DoctorResponse create(DoctorCreateRequest dto);
    DoctorResponse update(UUID id, DoctorUpdateRequest dto);
    DoctorResponse getById(UUID id);
    List<DoctorResponse> getAll();
    void delete(UUID id);
}
