package com.holisun.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class EquipmentRequest {

    @NotBlank
    private String name;

    @NotNull
    private UUID equipmentTypeId;

    private UUID roomId; // optional — mobile equipment may have no fixed room

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getEquipmentTypeId() {
        return equipmentTypeId;
    }

    public void setEquipmentTypeId(UUID equipmentTypeId) {
        this.equipmentTypeId = equipmentTypeId;
    }

    public UUID getRoomId() {
        return roomId;
    }

    public void setRoomId(UUID roomId) {
        this.roomId = roomId;
    }
}