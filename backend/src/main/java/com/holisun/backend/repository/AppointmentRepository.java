package com.holisun.backend.repository;

import com.holisun.backend.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    List<Appointment> findByPatientIdOrderByStartTimeDesc(UUID patientId);

    /**
     * Pacientii care au cel putin o fisa de consultatie. Ecranul de pacienti afiseaza butonul
     * "Fise" doar pentru ei, iar o intrebare pe pacient ar insemna cate o cerere pe rand de tabel.
     */
    @Query("""
            SELECT DISTINCT a.patient.id FROM Appointment a
            WHERE EXISTS (
                SELECT 1 FROM ConsultationRecord r WHERE r.appointmentId = a.id
            )
            """)
    List<UUID> findPatientIdsWithConsultationRecords();

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

    @Query("""
        SELECT a.id
        FROM Appointment a
        WHERE a.status = com.holisun.backend.enums.AppointmentStatus.COMPLETED
          AND a.completedAt IS NOT NULL
          AND a.completedAt <= :cutoff
          AND EXISTS (
              SELECT cr.id
              FROM ConsultationRecord cr
              WHERE cr.appointmentId = a.id
                AND cr.locked = false
          )
        """)
    List<UUID> findAppointmentIdsReadyForRecordLock(
            @Param("cutoff") LocalDateTime cutoff
    );
}