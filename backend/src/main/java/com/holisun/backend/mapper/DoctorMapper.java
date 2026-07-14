package com.holisun.backend.mapper;

import com.holisun.backend.dto.DoctorCreateRequest;
import com.holisun.backend.dto.DoctorResponse;
import com.holisun.backend.dto.DoctorUpdateRequest;
import com.holisun.backend.dto.WorkingHoursDto;
import com.holisun.backend.embeddables.WorkingHours;
import com.holisun.backend.entity.Doctor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface DoctorMapper {


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "weeklySchedule", source = "schedule")
    Doctor toEntity(DoctorCreateRequest dto);


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "weeklySchedule", source = "schedule")
    void updateEntityFromDto(DoctorUpdateRequest dto, @MappingTarget Doctor doctor);


    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "schedule", source = "weeklySchedule")
    @Mapping(target = "fullName", source = "user.username")
    DoctorResponse toResponse(Doctor doctor);


    WorkingHours toEntity(WorkingHoursDto dto);
    WorkingHoursDto toDto(WorkingHours hours);
}