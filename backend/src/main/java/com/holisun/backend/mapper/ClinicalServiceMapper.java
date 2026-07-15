package com.holisun.backend.mapper;

import com.holisun.backend.dto.ClinicalServiceRequest;
import com.holisun.backend.dto.ClinicalServiceResponse;
import com.holisun.backend.entity.Service;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "Spring", uses = EquipmentMapper.class)
public abstract class ClinicalServiceMapper {
    @Mapping(target = "requiredEquipmentTypes", ignore = true)
    public abstract Service dtoToClinicalService(ClinicalServiceRequest dto);

    public abstract ClinicalServiceResponse clinicalServiceToDto(Service clinicalService);

}
