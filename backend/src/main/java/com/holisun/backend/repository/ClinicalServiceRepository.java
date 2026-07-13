package com.holisun.backend.repository;

import com.holisun.backend.entity.ClinicalService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClinicalServiceRepository extends JpaRepository<ClinicalService, UUID> {
}