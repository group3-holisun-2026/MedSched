package com.holisun.backend.mapper;

import com.holisun.backend.dto.DoctorCreateRequest;
import com.holisun.backend.dto.DoctorResponse;
import com.holisun.backend.dto.DoctorUpdateRequest;
import com.holisun.backend.dto.WorkingHoursDto;
import com.holisun.backend.entity.Doctor;
import com.holisun.backend.entity.WorkSchedule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.time.DayOfWeek;

@Mapper(componentModel = "spring")
public interface DoctorMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "active", ignore = true) // new doctors default active = true in the entity
    @Mapping(target = "weeklySchedule", source = "schedule")
    Doctor toEntity(DoctorCreateRequest dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "active", ignore = true) // active toggled via a dedicated endpoint, not update
    @Mapping(target = "weeklySchedule", source = "schedule")
    void updateEntityFromDto(DoctorUpdateRequest dto, @MappingTarget Doctor doctor);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "schedule", source = "weeklySchedule")
    @Mapping(target = "fullName", source = "user.username")
    DoctorResponse toResponse(Doctor doctor);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "doctor", ignore = true) // back-reference set explicitly in DoctorServiceImpl before save
    WorkSchedule toEntity(WorkingHoursDto dto);

    WorkingHoursDto toDto(WorkSchedule schedule);

    default DayOfWeek map(Short value) {
        return value == null ? null : DayOfWeek.of(value);
    }

    default Short map(DayOfWeek value) {
        return value == null ? null : (short) value.getValue();
    }
}