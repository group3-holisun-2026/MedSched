package com.holisun.backend.mapper.summary;

import com.holisun.backend.dto.summary.DoctorSummary;
import com.holisun.backend.entity.Doctor;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DoctorSummaryMapper {
    DoctorSummary toSummary(Doctor doctor);
}
