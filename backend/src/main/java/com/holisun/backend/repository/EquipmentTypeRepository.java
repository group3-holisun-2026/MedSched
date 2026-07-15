package com.holisun.backend.repository;

import com.holisun.backend.entity.EquipmentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EquipmentTypeRepository extends JpaRepository<EquipmentType, UUID> {
    boolean existsByName(String name);
}
