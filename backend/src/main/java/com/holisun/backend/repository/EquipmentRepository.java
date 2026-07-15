package com.holisun.backend.repository;

import com.holisun.backend.entity.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EquipmentRepository extends JpaRepository<Equipment, UUID> {
    boolean existsByName(String name);
}
