package com.holisun.backend.service;

import com.holisun.backend.entity.Appointment;
import com.holisun.backend.enums.AppointmentStatus;
import com.holisun.backend.repository.AppointmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AppointmentUpdateIntegrationTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    // Folosim @Spy ca să integrăm logica REALĂ a mașinii de stări (nu una simulată)
    @Spy
    private AppointmentStateMachine stateMachine = new AppointmentStateMachine();

    // Aici injectăm ambele de mai sus direct în serviciul pe care îl testăm
    @InjectMocks
    private AppointmentService appointmentService;

    @Test
    void givenCompletedAppointment_whenUpdate_thenThrowsConflict() {
        UUID appointmentId = UUID.randomUUID();

        // 1. Extragem o programare "din baza de date" care are deja statusul COMPLETED
        Appointment appointment = new Appointment();
        appointment.setId(appointmentId);
        appointment.setStatus(AppointmentStatus.COMPLETED);

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        // 2. Apelăm metoda de update și ne așteptăm să arunce ResponseStatusException
        // Notă: Dacă metoda colegilor tăi de update() necesită un DTO valid în loc de null,
        // poți înlocui 'null' cu crearea acelui DTO, dar validarea stării ar trebui să pice oricum prima.
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> appointmentService.update(appointmentId, null),
                "Trebuia să arunce eroare la încercarea de a edita o programare COMPLETED!");

        // 3. Verificăm strict că eroarea este 409 CONFLICT, așa cum cere business-ul
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }
}