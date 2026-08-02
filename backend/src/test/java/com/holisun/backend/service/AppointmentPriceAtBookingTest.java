package com.holisun.backend.service;

import com.holisun.backend.dto.AppointmentRequest;
import com.holisun.backend.entity.Appointment;
import com.holisun.backend.entity.Doctor;
import com.holisun.backend.entity.Patient;
import com.holisun.backend.entity.Room;
import com.holisun.backend.entity.Service;
import com.holisun.backend.enums.AppointmentStatus;
import com.holisun.backend.mapper.AppointmentMapper;
import com.holisun.backend.repository.AppointmentRepository;
import com.holisun.backend.repository.DoctorRepository;
import com.holisun.backend.repository.EquipmentRepository;
import com.holisun.backend.repository.PatientRepository;
import com.holisun.backend.repository.RoomRepository;
import com.holisun.backend.repository.ServiceRepository;
import com.holisun.backend.service.notification.NotificationOutboxService;
import com.holisun.backend.util.AppointmentStateMachine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * B7 — pretul serviciului se ingheata la rezervare, ca raportul de vanzari sa nu se rescrie
 * retroactiv cand adminul schimba un tarif. Testele de mai jos fixeaza cele trei reguli:
 * se fotografiaza la creare, supravietuieste unei reprogramari pe acelasi serviciu si se
 * refotografiaza doar daca reprogramarea schimba serviciul.
 */
@ExtendWith(MockitoExtension.class)
class AppointmentPriceAtBookingTest {

    @Mock private PatientRepository patientRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private EquipmentRepository equipmentRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private AppointmentMapper appointmentMapper;
    @Mock private AvailabilityValidatorService availabilityValidatorService;
    @Mock private EquipmentAllocationService equipmentAllocationService;
    @Mock private AppointmentStateMachine appointmentStateMachine;
    @Mock private NotificationOutboxService notificationOutboxService;

    @InjectMocks private AppointmentService appointmentService;

    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID DOCTOR_ID = UUID.randomUUID();
    private static final UUID ROOM_ID = UUID.randomUUID();
    private static final LocalDateTime START = LocalDateTime.of(2026, 7, 1, 10, 0);

    @Test
    void createSnapshotsCurrentServicePrice() {
        Service service = service(new BigDecimal("250.00"));
        stubLookups(service);
        given(appointmentRepository.save(any(Appointment.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        appointmentService.create(request(service.getId(), START));

        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository).save(captor.capture());
        assertEquals(0, new BigDecimal("250.00").compareTo(captor.getValue().getPriceAtBooking()),
                "pretul trebuia fotografiat din serviciu la creare");
    }

    @Test
    void rescheduleKeepsThePriceAgreedAtBooking() {
        UUID appointmentId = UUID.randomUUID();
        Service service = service(new BigDecimal("400.00")); // tariful a crescut intre timp

        Appointment existing = existingAppointment(service, new BigDecimal("250.00"));
        given(appointmentRepository.findById(appointmentId)).willReturn(Optional.of(existing));
        stubLookups(service);
        given(appointmentRepository.save(any(Appointment.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        appointmentService.update(appointmentId, request(service.getId(), START.plusDays(1)));

        assertEquals(0, new BigDecimal("250.00").compareTo(existing.getPriceAtBooking()),
                "reprogramarea pe acelasi serviciu nu are voie sa reevalueze pretul");
    }

    @Test
    void changingTheServiceReSnapshotsThePrice() {
        UUID appointmentId = UUID.randomUUID();
        Service oldService = service(new BigDecimal("250.00"));
        Service newService = service(new BigDecimal("90.00"));

        Appointment existing = existingAppointment(oldService, new BigDecimal("250.00"));
        given(appointmentRepository.findById(appointmentId)).willReturn(Optional.of(existing));
        stubLookups(newService);
        given(appointmentRepository.save(any(Appointment.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        appointmentService.update(appointmentId, request(newService.getId(), START));

        assertEquals(0, new BigDecimal("90.00").compareTo(existing.getPriceAtBooking()),
                "pretul vechi nu mai descrie nimic daca s-a schimbat serviciul");
    }

    private void stubLookups(Service service) {
        given(patientRepository.findById(PATIENT_ID)).willReturn(Optional.of(new Patient()));
        given(doctorRepository.findById(DOCTOR_ID)).willReturn(Optional.of(new Doctor()));
        given(roomRepository.findById(ROOM_ID)).willReturn(Optional.of(new Room()));
        given(serviceRepository.findById(service.getId())).willReturn(Optional.of(service));
    }

    private Appointment existingAppointment(Service service, BigDecimal priceAtBooking) {
        Appointment appointment = new Appointment();
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointment.setService(service);
        appointment.setPriceAtBooking(priceAtBooking);
        appointment.setStartTime(START);
        appointment.setEndTime(START.plusMinutes(30));
        return appointment;
    }

    private static Service service(BigDecimal price) {
        Service service = new Service();
        service.setId(UUID.randomUUID());
        service.setName("Consult");
        service.setPrice(price);
        service.setDefaultDurationMinutes(30);
        return service;
    }

    private static AppointmentRequest request(UUID serviceId, LocalDateTime startTime) {
        return new AppointmentRequest(PATIENT_ID, DOCTOR_ID, ROOM_ID, serviceId, startTime, null);
    }
}
