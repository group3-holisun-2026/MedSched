package com.holisun.backend.repository;

import com.holisun.backend.entity.WorkSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkScheduleRepository extends JpaRepository<WorkSchedule, UUID> {
    List<WorkSchedule> findByDoctorId(UUID doctorId);
}
