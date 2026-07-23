package com.holisun.backend.repository;

import com.holisun.backend.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    @Query("""
            SELECT a FROM Appointment a
            WHERE a.doctor.id = :doctorId
              AND a.status NOT IN (com.holisun.backend.enums.AppointmentStatus.CANCELLED, com.holisun.backend.enums.AppointmentStatus.NO_SHOW)
              AND a.startTime < :end AND a.endTime > :start
              AND (:excludeAppointmentId IS NULL OR a.id <> :excludeAppointmentId)
            """)
    List<Appointment> findOverlappingForDoctor(@Param("doctorId") UUID doctorId,
                                                @Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end,
                                                @Param("excludeAppointmentId") UUID excludeAppointmentId);

    @Query("""
            SELECT a FROM Appointment a
            WHERE a.room.id = :roomId
              AND a.status NOT IN (com.holisun.backend.enums.AppointmentStatus.CANCELLED, com.holisun.backend.enums.AppointmentStatus.NO_SHOW)
              AND a.startTime < :end AND a.endTime > :start
              AND (:excludeAppointmentId IS NULL OR a.id <> :excludeAppointmentId)
            """)
    List<Appointment> findOverlappingForRoom(@Param("roomId") UUID roomId,
                                              @Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end,
                                              @Param("excludeAppointmentId") UUID excludeAppointmentId);

    @Query("""
            SELECT a FROM Appointment a
            WHERE a.equipment.id = :equipmentId
              AND a.status NOT IN (com.holisun.backend.enums.AppointmentStatus.CANCELLED, com.holisun.backend.enums.AppointmentStatus.NO_SHOW)
              AND a.startTime < :end AND a.endTime > :start
              AND (:excludeAppointmentId IS NULL OR a.id <> :excludeAppointmentId)
            """)
    List<Appointment> findOverlappingForEquipment(@Param("equipmentId") UUID equipmentId,
                                                   @Param("start") LocalDateTime start,
                                                   @Param("end") LocalDateTime end,
                                                   @Param("excludeAppointmentId") UUID excludeAppointmentId);

    // P4:
    List<Appointment> findByDoctorIdInAndStartTimeLessThanAndEndTimeGreaterThan(List<UUID> doctorIds, LocalDateTime end, LocalDateTime start);

    List<Appointment> findByRoomIdAndStartTimeBetween(UUID roomId, LocalDateTime from, LocalDateTime to);
}