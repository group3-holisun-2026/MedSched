package com.holisun.backend.repository;

import com.holisun.backend.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClinicalServiceRepository extends JpaRepository<Service, UUID> {
    List<Service> findByName(String name);
}
