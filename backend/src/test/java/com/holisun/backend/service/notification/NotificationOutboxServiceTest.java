package com.holisun.backend.service.notification;

import com.holisun.backend.entity.Appointment;
import com.holisun.backend.entity.Doctor;
import com.holisun.backend.entity.Notification;
import com.holisun.backend.entity.Patient;
import com.holisun.backend.entity.Room;
import com.holisun.backend.entity.User;
import com.holisun.backend.enums.NotificationStatus;
import com.holisun.backend.enums.NotificationTrigger;
import com.holisun.backend.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Regulile de inserare in outbox (F-502): ce randuri apar la creare, mutare si anulare.
 * Comportamentul dispatcher-ului (retry/backoff, NFR-2) e testat separat, in
 * {@code NotificationDispatcherOutboxTest}.
 */
@ExtendWith(MockitoExtension.class)
class NotificationOutboxServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationMessageFactory messageFactory;

    @InjectMocks
    private NotificationOutboxServiceImpl outboxService;

    private Appointment appointment;

    @BeforeEach
    void setUp() {
        messageFactory = new NotificationMessageFactory();
        ReflectionTestUtils.setField(messageFactory, "baseUrl", "http://localhost:5173");

        outboxService = new NotificationOutboxServiceImpl(notificationRepository, messageFactory);

        appointment = buildAppointment(LocalDateTime.now().plusDays(3));
    }

    private Appointment buildAppointment(LocalDateTime startTime) {
        User doctorUser = new User();
        doctorUser.setUsername("Popescu");

        Doctor doctor = new Doctor();
        doctor.setUser(doctorUser);

        Room room = new Room();
        room.setName("Cabinet 1");

        Patient patient = new Patient();
        patient.setFirstName("Ion");
        patient.setEmail("ion@exemplu.ro");

        Appointment a = new Appointment();
        a.setId(UUID.randomUUID());
        a.setPatient(patient);
        a.setDoctor(doctor);
        a.setRoom(room);
        a.setStartTime(startTime);
        a.setEndTime(startTime.plusMinutes(30));
        return a;
    }

    private List<Notification> savedNotifications() {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        return captor.getAllValues();
    }

    @Test
    void createSchedulesConfirmationAndReminderForAFutureAppointment() {
        outboxService.enqueueConfirmation(appointment);
        outboxService.enqueueReminder(appointment);

        List<Notification> saved = savedNotifications();

        assertThat(saved).hasSize(2);
        assertThat(saved).allSatisfy(n -> assertThat(n.getStatus()).isEqualTo(NotificationStatus.PENDING));
        assertThat(saved).extracting(Notification::getTrigger)
                .containsExactly(NotificationTrigger.CONFIRMATION, NotificationTrigger.REMINDER_24H);

        Notification reminder = saved.get(1);
        assertThat(reminder.getNextAttemptAt()).isEqualTo(appointment.getStartTime().minusHours(24));
        assertThat(reminder.getConfirmationToken()).isNotBlank();
        // Linkul trebuie sa duca pe ruta reala din frontend (/c/:token).
        assertThat(reminder.getBody()).contains("http://localhost:5173/c/" + reminder.getConfirmationToken());
    }

    @Test
    void appointmentInsideTheNextDayGetsNoReminder() {
        Appointment soon = buildAppointment(LocalDateTime.now().plusHours(2));

        outboxService.enqueueConfirmation(soon);
        outboxService.enqueueReminder(soon);

        List<Notification> saved = savedNotifications();

        // Doar confirmarea: un "reminder cu 24h inainte" pentru o programare de peste 2 ore
        // ar pleca imediat dupa confirmare, deci ar fi zgomot.
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getTrigger()).isEqualTo(NotificationTrigger.CONFIRMATION);
    }

    @Test
    void rescheduleCancelsTheOldReminderAndSchedulesANewOne() {
        Notification oldReminder = new Notification();
        oldReminder.setTrigger(NotificationTrigger.REMINDER_24H);
        oldReminder.setStatus(NotificationStatus.PENDING);

        given(notificationRepository.findByAppointmentIdAndTriggerAndStatus(
                eq(appointment.getId()), eq(NotificationTrigger.REMINDER_24H), eq(NotificationStatus.PENDING)))
                .willReturn(List.of(oldReminder));

        outboxService.enqueueRescheduled(appointment);

        assertThat(oldReminder.getStatus()).isEqualTo(NotificationStatus.CANCELLED);

        List<Notification> saved = savedNotifications();
        assertThat(saved).extracting(Notification::getTrigger)
                .contains(NotificationTrigger.RESCHEDULED, NotificationTrigger.REMINDER_24H);

        // Atentie: captorul prinde si salvarea reminder-ului vechi (trecut pe CANCELLED),
        // deci reminder-ul nou se identifica dupa status, nu doar dupa trigger.
        Notification newReminder = saved.stream()
                .filter(n -> n.getTrigger() == NotificationTrigger.REMINDER_24H)
                .filter(n -> n.getStatus() == NotificationStatus.PENDING)
                .findFirst()
                .orElseThrow();
        assertThat(newReminder.getNextAttemptAt()).isEqualTo(appointment.getStartTime().minusHours(24));
        assertThat(newReminder.getStatus()).isEqualTo(NotificationStatus.PENDING);
    }

    @Test
    void cancelCancelsThePendingReminderAndQueuesACancellationNotice() {
        Notification oldReminder = new Notification();
        oldReminder.setTrigger(NotificationTrigger.REMINDER_24H);
        oldReminder.setStatus(NotificationStatus.PENDING);

        given(notificationRepository.findByAppointmentIdAndTriggerAndStatus(
                any(), eq(NotificationTrigger.REMINDER_24H), eq(NotificationStatus.PENDING)))
                .willReturn(List.of(oldReminder));

        outboxService.enqueueCancelled(appointment);

        assertThat(oldReminder.getStatus()).isEqualTo(NotificationStatus.CANCELLED);

        List<Notification> saved = savedNotifications();
        assertThat(saved).extracting(Notification::getTrigger)
                .contains(NotificationTrigger.CANCELLED);

        // La anulare nu se mai programeaza nicio reamintire noua (spre deosebire de reprogramare).
        assertThat(saved)
                .filteredOn(n -> n.getTrigger() == NotificationTrigger.REMINDER_24H)
                .allSatisfy(n -> assertThat(n.getStatus()).isEqualTo(NotificationStatus.CANCELLED));
    }

    @Test
    void anInvalidEmailFailsTheNotificationInsteadOfBreakingTheBooking() {
        appointment.getPatient().setEmail("nu-e-o-adresa");

        outboxService.enqueueConfirmation(appointment);

        Notification saved = savedNotifications().get(0);
        // Randul se insereaza oricum: crearea unei programari nu trebuie sa pice pentru ca
        // pacientul are o adresa gresita.
        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(saved.getLastError()).contains("nu-e-o-adresa");
    }

    @Test
    void messageBodyCarriesNoClinicalDataAndNoDiacritics() {
        appointment.setNotes("Pacientul acuza dureri toracice");

        outboxService.enqueueConfirmation(appointment);

        String body = savedNotifications().get(0).getBody();

        assertThat(body).doesNotContain("dureri toracice");
        assertThat(body).contains("Popescu").contains("Cabinet 1");
        assertThat(body).matches("[\\x20-\\x7E]+");
    }
}
