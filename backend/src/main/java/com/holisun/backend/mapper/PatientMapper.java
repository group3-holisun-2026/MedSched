package com.holisun.backend.mapper;

import com.holisun.backend.dto.PatientQuickCreateRequest;
import com.holisun.backend.dto.PatientRequest;
import com.holisun.backend.dto.PatientResponse;
import com.holisun.backend.entity.Patient;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface PatientMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cnpHash", ignore = true)
    @Mapping(target = "profileComplete", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Patient dtoToPatient(PatientRequest dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cnpHash", ignore = true)
    @Mapping(target = "profileComplete", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Patient dtoToPatient(PatientQuickCreateRequest dto);

    PatientResponse patientToDto(Patient patient);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cnpHash", ignore = true)
    @Mapping(target = "profileComplete", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updatePatientFromDto(PatientRequest dto,
                              @MappingTarget Patient patient);
}
