package com.holisun.backend.repository;

import com.holisun.backend.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    @Query
    List<Appointment> findOverlappingForDoctor(UUID doctorId, LocalDateTime start, LocalDateTime end, UUID excludeAppointmentId);

    @Query
    List<Appointment> findOverlappingForRoom(UUID roomId, LocalDateTime start, LocalDateTime end, UUID excludeAppointmentId);

    @Query
    List<Appointment> findOverlappingForEquipment(UUID equipmentId, LocalDateTime start, LocalDateTime end, UUID excludeAppointmentId);
}
