package com.holisun.backend.dto;

import java.util.UUID;

public class EquipmentResponse {

    private UUID id;
    private String name;
    private boolean active;
    private UUID equipmentTypeId;
    private UUID roomId;

    public EquipmentResponse() {
    }

    public EquipmentResponse(UUID id, String name, boolean active, UUID equipmentTypeId, UUID roomId) {
        this.id = id;
        this.name = name;
        this.active = active;
        this.equipmentTypeId = equipmentTypeId;
        this.roomId = roomId;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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