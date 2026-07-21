package com.holisun.backend.repository;

import com.holisun.backend.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    List<Appointment> findOverlappingForDoctor(UUID doctorId, LocalDateTime start, LocalDateTime end, UUID excludeAppointmentId);

    List<Appointment> findOverlappingForRoom(UUID roomId, LocalDateTime start, LocalDateTime end, UUID excludeAppointmentId);

    List<Appointment> findOverlappingForEquipment(UUID equipmentId, LocalDateTime start, LocalDateTime end, UUID excludeAppointmentId);

    // P4:
    List<Appointment> findByDoctorIdInAndStartTimeLessThanAndEndTimeGreaterThan(List<UUID> doctorIds, LocalDateTime end, LocalDateTime start);

    List<Appointment> findByRoomIdAndStartTimeBetween(UUID roomId, LocalDateTime from, LocalDateTime to);


}
