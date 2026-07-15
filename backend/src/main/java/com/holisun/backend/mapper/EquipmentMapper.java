package com.holisun.backend.mapper;

import com.holisun.backend.dto.EquipmentResponse;
import com.holisun.backend.dto.EquipmentTypeResponse;
import com.holisun.backend.entity.Equipment;
import com.holisun.backend.entity.EquipmentType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EquipmentMapper {

    @Mapping(target = "equipmentTypeId", source = "equipmentType.id")
    @Mapping(target = "roomId", source = "room.id")
    EquipmentResponse toResponse(Equipment equipment);

    EquipmentTypeResponse toResponse(EquipmentType equipmentType);
}