package com.holisun.backend.repository;

import com.holisun.backend.entity.Appointment;
import org.springframework.data.repository.Repository;

import java.util.UUID;

import com.holisun.backend.dto.report.SalesRow;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import com.holisun.backend.repository.projection.PatientNoShowCounts;

import com.holisun.backend.repository.projection.WeekdayNoShowCounts;

import com.holisun.backend.repository.projection.ResourceMinutes;

public interface ReportRepository extends Repository<Appointment, UUID> {

    @Query("""
        SELECT new com.holisun.backend.dto.report.SalesRow(
            s.id,
            s.name,
            COUNT(a),
            SUM(a.priceAtBooking)
        )
        FROM Appointment a
        JOIN a.service s
        WHERE a.status = com.holisun.backend.enums.AppointmentStatus.COMPLETED
          AND a.startTime >= :from
          AND a.startTime < :to
        GROUP BY s.id, s.name
        ORDER BY SUM(a.priceAtBooking) DESC
        """)
    List<SalesRow> sumSalesByService(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
        SELECT new com.holisun.backend.dto.report.SalesRow(
            d.id,
            u.username,
            COUNT(a),
            SUM(a.priceAtBooking)
        )
        FROM Appointment a
        JOIN a.doctor d
        JOIN d.user u
        WHERE a.status = com.holisun.backend.enums.AppointmentStatus.COMPLETED
          AND a.startTime >= :from
          AND a.startTime < :to
        GROUP BY d.id, u.username
        ORDER BY SUM(a.priceAtBooking) DESC
        """)
    List<SalesRow> sumSalesByDoctor(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
        SELECT
            p.id AS patientId,
            CONCAT(CONCAT(p.firstName, ' '), p.lastName) AS patientName,
            COUNT(a) AS total,
            SUM(
                CASE
                    WHEN a.status = com.holisun.backend.enums.AppointmentStatus.NO_SHOW
                    THEN 1
                    ELSE 0
                END
            ) AS noShows
        FROM Appointment a
        JOIN a.patient p
        WHERE a.status <> com.holisun.backend.enums.AppointmentStatus.CANCELLED
          AND a.startTime >= :from
          AND a.startTime < :to
        GROUP BY p.id, p.firstName, p.lastName
        ORDER BY p.lastName, p.firstName
        """)
    List<PatientNoShowCounts> countNoShowsByPatient(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query(
            value = """
                SELECT
                    CAST(EXTRACT(ISODOW FROM start_time) AS INTEGER)
                        AS "dayOfWeekValue",
                    COUNT(*) AS total,
                    SUM(
                        CASE
                            WHEN status = 'NO_SHOW' THEN 1
                            ELSE 0
                        END
                    ) AS "noShows"
                FROM appointments
                WHERE status <> 'CANCELLED'
                  AND start_time >= :from
                  AND start_time < :to
                GROUP BY EXTRACT(ISODOW FROM start_time)
                ORDER BY EXTRACT(ISODOW FROM start_time)
                """,
            nativeQuery = true
    )
    List<WeekdayNoShowCounts> countNoShowsByWeekday(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query(
            value = """
                SELECT
                    doctor_id AS "resourceId",
                    COALESCE(
                        SUM(
                            EXTRACT(EPOCH FROM (end_time - start_time)) / 60
                        ),
                        0
                    ) AS "bookedMinutes"
                FROM appointments
                WHERE status NOT IN ('CANCELLED', 'NO_SHOW')
                  AND start_time >= :from
                  AND start_time < :to
                GROUP BY doctor_id
                """,
            nativeQuery = true
    )
    List<ResourceMinutes> sumBookedMinutesByDoctor(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query(
            value = """
                SELECT
                    room_id AS "resourceId",
                    COALESCE(
                        SUM(
                            EXTRACT(EPOCH FROM (end_time - start_time)) / 60
                        ),
                        0
                    ) AS "bookedMinutes"
                FROM appointments
                WHERE status NOT IN ('CANCELLED', 'NO_SHOW')
                  AND start_time >= :from
                  AND start_time < :to
                GROUP BY room_id
                """,
            nativeQuery = true
    )
    List<ResourceMinutes> sumBookedMinutesByRoom(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}