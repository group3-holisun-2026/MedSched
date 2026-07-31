package com.holisun.backend.controller;

import com.holisun.backend.dto.PublicAppointmentResponse;
import com.holisun.backend.entity.Appointment;
import com.holisun.backend.entity.Notification;
import com.holisun.backend.enums.AppointmentStatus;
import com.holisun.backend.repository.NotificationRepository;
import com.holisun.backend.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/public/appointments")
@RequiredArgsConstructor
public class PublicAppointmentController {

    private final NotificationRepository notificationRepository;
    private final AppointmentService appointmentService;

    @GetMapping("/{token}")
    public PublicAppointmentResponse getByToken(@PathVariable String token) {
        Notification notification = notificationRepository.findByConfirmationToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Token invalid"));

        Appointment appointment = notification.getAppointment();

        if (appointment.getStartTime().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Programarea a trecut");
        }

        AppointmentStatus status = appointment.getStatus();
        boolean canConfirm = status == AppointmentStatus.SCHEDULED;
        boolean canCancel = status == AppointmentStatus.SCHEDULED || status == AppointmentStatus.CONFIRMED;

        return new PublicAppointmentResponse(
                appointment.getPatient().getFirstName(),
                appointment.getStartTime(),
                appointment.getEndTime(),
                appointment.getDoctor().getUser().getUsername(),
                appointment.getRoom().getName(),
                status,
                canConfirm,
                canCancel
        );
    }

    @PostMapping("/{token}/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirm(@PathVariable String token) {
        Notification notification = notificationRepository.findByConfirmationToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Token invalid"));

        if (notification.getAppointment().getStartTime().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Programarea a trecut");
        }

        appointmentService.confirm(notification.getAppointment().getId());
    }

    @PostMapping("/{token}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable String token) {
        Notification notification = notificationRepository.findByConfirmationToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Token invalid"));

        if (notification.getAppointment().getStartTime().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Programarea a trecut");
        }

        appointmentService.cancel(notification.getAppointment().getId());
    }
}
