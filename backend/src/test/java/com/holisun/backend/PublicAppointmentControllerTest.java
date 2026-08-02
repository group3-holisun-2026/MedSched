package com.holisun.backend;

import com.holisun.backend.entity.Appointment;
import com.holisun.backend.entity.Doctor;
import com.holisun.backend.entity.Notification;
import com.holisun.backend.entity.Patient;
import com.holisun.backend.entity.Room;
import com.holisun.backend.entity.User;
import com.holisun.backend.enums.AppointmentStatus;
import com.holisun.backend.repository.NotificationRepository;
import com.holisun.backend.service.AppointmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class PublicAppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @MockitoBean
    private AppointmentService appointmentService;

    @Test
    public void getAppointment_invalidToken_returns404() throws Exception {
        when(notificationRepository.findByConfirmationToken("bad-token")).thenReturn(Optional.empty());

        mockMvc.perform(get("/public/appointments/bad-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void getAppointment_pastAppointment_returns410() throws Exception {
        Notification notification = new Notification();
        Appointment appointment = new Appointment();
        appointment.setStartTime(LocalDateTime.now().minusDays(1)); // In the past
        notification.setAppointment(appointment);

        when(notificationRepository.findByConfirmationToken("expired-token")).thenReturn(Optional.of(notification));

        mockMvc.perform(get("/public/appointments/expired-token"))
                .andExpect(status().isGone());
    }

    @Test
    public void getAppointment_validToken_returnsMappedDtoWithoutSensitiveData() throws Exception {
        Notification notification = new Notification();
        
        Patient patient = new Patient();
        patient.setFirstName("Ion");
        patient.setCnp("1234567890123");
        patient.setEmail("ion@test.com");
        patient.setPhone("0712345678");

        User doctorUser = new User();
        doctorUser.setUsername("Dr. Popescu");
        Doctor doctor = new Doctor();
        doctor.setUser(doctorUser);

        Room room = new Room();
        room.setName("Cabinet 1");

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setRoom(room);
        appointment.setStartTime(LocalDateTime.now().plusDays(2));
        appointment.setEndTime(LocalDateTime.now().plusDays(2).plusMinutes(30));
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointment.setNotes("Sensitive medical notes");
        
        notification.setAppointment(appointment);

        when(notificationRepository.findByConfirmationToken("valid-token")).thenReturn(Optional.of(notification));

        mockMvc.perform(get("/public/appointments/valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientFirstName").value("Ion"))
                .andExpect(jsonPath("$.doctorName").value("Dr. Popescu"))
                .andExpect(jsonPath("$.roomName").value("Cabinet 1"))
                .andExpect(jsonPath("$.canConfirm").value(true))
                .andExpect(jsonPath("$.canCancel").value(true))
                // Verify sensitive fields are missing (privacy check)
                .andExpect(jsonPath("$.cnp").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.phone").doesNotExist())
                .andExpect(jsonPath("$.notes").doesNotExist());
    }
}
