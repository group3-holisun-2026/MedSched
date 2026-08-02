package com.holisun.backend.service.report;

import com.holisun.backend.config.ReportProperties;
import com.holisun.backend.dto.report.*;
import com.holisun.backend.entity.Doctor;
import com.holisun.backend.entity.Room;
import com.holisun.backend.entity.WorkSchedule;
import com.holisun.backend.repository.DoctorRepository;
import com.holisun.backend.repository.ReportRepository;
import com.holisun.backend.repository.RoomRepository;
import com.holisun.backend.repository.WorkScheduleRepository;
import com.holisun.backend.repository.projection.PatientNoShowCounts;
import com.holisun.backend.repository.projection.ResourceMinutes;
import com.holisun.backend.repository.projection.WeekdayNoShowCounts;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final DoctorRepository doctorRepository;
    private final RoomRepository roomRepository;
    private final WorkScheduleRepository workScheduleRepository;
    private final ReportProperties reportProperties;

    public SalesReportResponse sales(LocalDate from, LocalDate to) {
        DateRange range = validateRange(from, to);

        List<SalesRow> byService = reportRepository.sumSalesByService(
                range.from(),
                range.toExclusive()
        );

        List<SalesRow> byDoctor = reportRepository.sumSalesByDoctor(
                range.from(),
                range.toExclusive()
        );

        BigDecimal grandTotal = byService.stream()
                .map(SalesRow::total)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SalesReportResponse(
                from,
                to,
                grandTotal,
                byService,
                byDoctor
        );
    }

    public NoShowReportResponse noShow(LocalDate from, LocalDate to) {
        DateRange range = validateRange(from, to);

        List<PatientNoShowRow> byPatient =
                reportRepository.countNoShowsByPatient(
                                range.from(),
                                range.toExclusive()
                        )
                        .stream()
                        .map(row -> new PatientNoShowRow(
                                row.getPatientId(),
                                row.getPatientName(),
                                row.getTotal(),
                                row.getNoShows(),
                                calculateRate(
                                        row.getNoShows(),
                                        row.getTotal()
                                )
                        ))
                        .toList();

        long total = byPatient.stream()
                .mapToLong(PatientNoShowRow::total)
                .sum();

        long noShows = byPatient.stream()
                .mapToLong(PatientNoShowRow::noShows)
                .sum();

        Map<Integer, WeekdayNoShowCounts> countsByDay =
                reportRepository.countNoShowsByWeekday(
                                range.from(),
                                range.toExclusive()
                        )
                        .stream()
                        .collect(Collectors.toMap(
                                WeekdayNoShowCounts::getDayOfWeekValue,
                                Function.identity()
                        ));

        List<WeekdayNoShowRow> byWeekday =
                Arrays.stream(DayOfWeek.values())
                        .map(day -> {
                            WeekdayNoShowCounts counts =
                                    countsByDay.get(day.getValue());

                            long dayTotal =
                                    counts == null ? 0 : counts.getTotal();

                            long dayNoShows =
                                    counts == null ? 0 : counts.getNoShows();

                            return new WeekdayNoShowRow(
                                    day,
                                    dayTotal,
                                    dayNoShows,
                                    calculateRate(dayNoShows, dayTotal)
                            );
                        })
                        .toList();

        return new NoShowReportResponse(
                from,
                to,
                total,
                noShows,
                calculateRate(noShows, total),
                byPatient,
                byWeekday
        );
    }

    public OccupancyReportResponse occupancy(
            LocalDate from,
            LocalDate to
    ) {
        DateRange range = validateRange(from, to);

        Map<UUID, Long> bookedByDoctor = toBookedMinutesMap(
                reportRepository.sumBookedMinutesByDoctor(
                        range.from(),
                        range.toExclusive()
                )
        );

        Map<UUID, Long> bookedByRoom = toBookedMinutesMap(
                reportRepository.sumBookedMinutesByRoom(
                        range.from(),
                        range.toExclusive()
                )
        );

        List<Doctor> activeDoctors =
                doctorRepository.findByActiveTrue();

        List<UUID> doctorIds = activeDoctors.stream()
                .map(Doctor::getId)
                .toList();

        List<WorkSchedule> schedules = doctorIds.isEmpty()
                ? List.of()
                : workScheduleRepository.findByDoctorIdIn(doctorIds);

        Map<UUID, List<WorkSchedule>> schedulesByDoctor =
                schedules.stream()
                        .collect(Collectors.groupingBy(
                                schedule -> schedule.getDoctor().getId()
                        ));

        List<ResourceOccupancyRow> doctorRows =
                activeDoctors.stream()
                        .map(doctor -> {
                            long booked = bookedByDoctor.getOrDefault(
                                    doctor.getId(),
                                    0L
                            );

                            long available = calculateDoctorAvailability(
                                    from,
                                    to,
                                    schedulesByDoctor.getOrDefault(
                                            doctor.getId(),
                                            List.of()
                                    )
                            );

                            return new ResourceOccupancyRow(
                                    doctor.getId(),
                                    doctor.getUser().getUsername(),
                                    booked,
                                    available,
                                    calculateRate(booked, available)
                            );
                        })
                        .sorted(Comparator.comparing(
                                ResourceOccupancyRow::name,
                                String.CASE_INSENSITIVE_ORDER
                        ))
                        .toList();

        long roomAvailability =
                calculateRoomAvailability(from, to);

        List<ResourceOccupancyRow> roomRows =
                roomRepository.findByActiveTrue()
                        .stream()
                        .map(room -> {
                            long booked = bookedByRoom.getOrDefault(
                                    room.getId(),
                                    0L
                            );

                            return new ResourceOccupancyRow(
                                    room.getId(),
                                    room.getName(),
                                    booked,
                                    roomAvailability,
                                    calculateRate(
                                            booked,
                                            roomAvailability
                                    )
                            );
                        })
                        .sorted(Comparator.comparing(
                                ResourceOccupancyRow::name,
                                String.CASE_INSENSITIVE_ORDER
                        ))
                        .toList();

        return new OccupancyReportResponse(
                from,
                to,
                doctorRows,
                roomRows
        );
    }

    private DateRange validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Datele from si to sunt obligatorii."
            );
        }

        if (from.isAfter(to)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Data from nu poate fi dupa data to."
            );
        }

        long numberOfDays =
                ChronoUnit.DAYS.between(from, to) + 1;

        if (numberOfDays > 366) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Intervalul raportului nu poate depasi 366 de zile."
            );
        }

        return new DateRange(
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay()
        );
    }

    private Map<UUID, Long> toBookedMinutesMap(
            List<ResourceMinutes> rows
    ) {
        return rows.stream()
                .collect(Collectors.toMap(
                        ResourceMinutes::getResourceId,
                        row -> Math.round(row.getBookedMinutes()),
                        Long::sum
                ));
    }

    private long calculateDoctorAvailability(
            LocalDate from,
            LocalDate to,
            List<WorkSchedule> schedules
    ) {
        long totalMinutes = 0;

        for (
                LocalDate date = from;
                !date.isAfter(to);
                date = date.plusDays(1)
        ) {
            for (WorkSchedule schedule : schedules) {
                if (schedule.getDayOfWeek() == date.getDayOfWeek()) {
                    totalMinutes += positiveMinutesBetween(
                            schedule.getStartTime(),
                            schedule.getEndTime()
                    );
                }
            }
        }

        return totalMinutes;
    }

    private long calculateRoomAvailability(
            LocalDate from,
            LocalDate to
    ) {
        long minutesPerDay = positiveMinutesBetween(
                reportProperties.getClinicOpenTime(),
                reportProperties.getClinicCloseTime()
        );

        long workingDays = from
                .datesUntil(to.plusDays(1))
                .filter(date ->
                        reportProperties
                                .getClinicWorkingDays()
                                .contains(date.getDayOfWeek())
                )
                .count();

        return minutesPerDay * workingDays;
    }

    private long positiveMinutesBetween(
            LocalTime start,
            LocalTime end
    ) {
        return Math.max(
                0,
                Duration.between(start, end).toMinutes()
        );
    }

    private double calculateRate(long numerator, long denominator) {
        if (denominator == 0) {
            return 0.0;
        }

        return (double) numerator / denominator;
    }

    private record DateRange(
            LocalDateTime from,
            LocalDateTime toExclusive
    ) {
    }
}