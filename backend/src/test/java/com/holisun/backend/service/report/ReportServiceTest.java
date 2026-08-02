package com.holisun.backend.service.report;

import com.holisun.backend.config.ReportProperties;
import com.holisun.backend.dto.report.*;
import com.holisun.backend.entity.Doctor;
import com.holisun.backend.entity.User;
import com.holisun.backend.entity.WorkSchedule;
import com.holisun.backend.repository.*;
import com.holisun.backend.repository.projection.PatientNoShowCounts;
import com.holisun.backend.repository.projection.ResourceMinutes;
import com.holisun.backend.repository.projection.WeekdayNoShowCounts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private WorkScheduleRepository workScheduleRepository;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(
                reportRepository,
                doctorRepository,
                roomRepository,
                workScheduleRepository,
                new ReportProperties()
        );
    }

    @Test
    void calculatesDoctorOccupancyAndHandlesDoctorWithoutSchedule() {
        LocalDate monday = LocalDate.of(2026, 7, 6);

        UUID scheduledDoctorId = UUID.randomUUID();
        UUID doctorWithoutScheduleId = UUID.randomUUID();

        Doctor scheduledDoctor = createDoctor(
                scheduledDoctorId,
                "doctor-one"
        );

        Doctor doctorWithoutSchedule = createDoctor(
                doctorWithoutScheduleId,
                "doctor-zero"
        );

        WorkSchedule schedule = new WorkSchedule();
        schedule.setDoctor(scheduledDoctor);
        schedule.setDayOfWeek(DayOfWeek.MONDAY);
        schedule.setStartTime(LocalTime.of(8, 0));
        schedule.setEndTime(LocalTime.of(12, 0));

        ResourceMinutes bookedMinutes = mock(ResourceMinutes.class);

        given(bookedMinutes.getResourceId())
                .willReturn(scheduledDoctorId);

        given(bookedMinutes.getBookedMinutes())
                .willReturn(60.0);

        given(reportRepository.sumBookedMinutesByDoctor(any(), any()))
                .willReturn(List.of(bookedMinutes));

        given(reportRepository.sumBookedMinutesByRoom(any(), any()))
                .willReturn(List.of());

        given(doctorRepository.findByActiveTrue())
                .willReturn(List.of(
                        scheduledDoctor,
                        doctorWithoutSchedule
                ));

        given(workScheduleRepository.findByDoctorIdIn(anyList()))
                .willReturn(List.of(schedule));

        given(roomRepository.findByActiveTrue())
                .willReturn(List.of());

        OccupancyReportResponse result =
                reportService.occupancy(monday, monday);

        ResourceOccupancyRow scheduledRow =
                findDoctor(result, scheduledDoctorId);

        assertEquals(60, scheduledRow.bookedMinutes());
        assertEquals(240, scheduledRow.availableMinutes());
        assertEquals(0.25, scheduledRow.occupancyRate(), 0.0001);

        ResourceOccupancyRow noScheduleRow =
                findDoctor(result, doctorWithoutScheduleId);

        assertEquals(0, noScheduleRow.bookedMinutes());
        assertEquals(0, noScheduleRow.availableMinutes());
        assertEquals(0.0, noScheduleRow.occupancyRate());
        assertFalse(Double.isNaN(noScheduleRow.occupancyRate()));
    }

    @Test
    void calculatesNoShowTotalsAndRates() {
        UUID patientId = UUID.randomUUID();

        PatientNoShowCounts patient =
                mock(PatientNoShowCounts.class);

        given(patient.getPatientId()).willReturn(patientId);
        given(patient.getPatientName()).willReturn("Ana");
        given(patient.getTotal()).willReturn(4L);
        given(patient.getNoShows()).willReturn(1L);

        WeekdayNoShowCounts monday =
                mock(WeekdayNoShowCounts.class);

        given(monday.getDayOfWeekValue())
                .willReturn(DayOfWeek.MONDAY.getValue());

        given(monday.getTotal()).willReturn(4L);
        given(monday.getNoShows()).willReturn(1L);

        given(reportRepository.countNoShowsByPatient(any(), any()))
                .willReturn(List.of(patient));

        given(reportRepository.countNoShowsByWeekday(any(), any()))
                .willReturn(List.of(monday));

        NoShowReportResponse result = reportService.noShow(
                LocalDate.of(2026, 7, 6),
                LocalDate.of(2026, 7, 6)
        );

        assertEquals(4, result.total());
        assertEquals(1, result.noShows());
        assertEquals(0.25, result.rate(), 0.0001);
        assertEquals(7, result.byWeekday().size());

        WeekdayNoShowRow mondayRow = result.byWeekday()
                .stream()
                .filter(row ->
                        row.dayOfWeek() == DayOfWeek.MONDAY
                )
                .findFirst()
                .orElseThrow();

        assertEquals(0.25, mondayRow.rate(), 0.0001);
    }

    @Test
    void salesGrandTotalEqualsSumOfServiceRows() {
        SalesRow first = new SalesRow(
                UUID.randomUUID(),
                "Consultatie",
                2,
                new BigDecimal("100.50")
        );

        SalesRow second = new SalesRow(
                UUID.randomUUID(),
                "Ecografie",
                1,
                new BigDecimal("50.00")
        );

        given(reportRepository.sumSalesByService(any(), any()))
                .willReturn(List.of(first, second));

        given(reportRepository.sumSalesByDoctor(any(), any()))
                .willReturn(List.of());

        SalesReportResponse result = reportService.sales(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31)
        );

        assertEquals(
                new BigDecimal("150.50"),
                result.grandTotal()
        );
    }

    private Doctor createDoctor(UUID id, String username) {
        User user = new User();
        user.setUsername(username);

        Doctor doctor = new Doctor();
        doctor.setId(id);
        doctor.setUser(user);

        return doctor;
    }

    private ResourceOccupancyRow findDoctor(
            OccupancyReportResponse report,
            UUID doctorId
    ) {
        return report.doctors()
                .stream()
                .filter(row ->
                        row.resourceId().equals(doctorId)
                )
                .findFirst()
                .orElseThrow();
    }
}