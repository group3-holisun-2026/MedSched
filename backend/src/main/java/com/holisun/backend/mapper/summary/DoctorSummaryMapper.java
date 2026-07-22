package com.holisun.backend.mapper.summary;

import com.holisun.backend.dto.summary.DoctorSummary;
import com.holisun.backend.entity.Doctor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DoctorSummaryMapper {
    @Mapping(target = "username", source = "user.username")
    DoctorSummary toSummary(Doctor doctor);
}
