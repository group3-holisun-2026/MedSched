package com.holisun.backend.mapper;

import com.holisun.backend.dto.EquipmentResponse;
import com.holisun.backend.entity.Equipment;
import com.holisun.backend.entity.EquipmentType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EquipmentMapper {

    EquipmentResponse toResponse(Equipment equipment);

    @Mapping(target = "active", constant = "true") // EquipmentType has no active flag — see note below
    EquipmentResponse toResponse(EquipmentType equipmentType);
}