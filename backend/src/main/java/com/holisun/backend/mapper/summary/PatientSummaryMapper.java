package com.holisun.backend.mapper.summary;

import com.holisun.backend.dto.summary.PatientSummary;
import com.holisun.backend.entity.Patient;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PatientSummaryMapper {
    PatientSummary toSummary(Patient patient);
}
