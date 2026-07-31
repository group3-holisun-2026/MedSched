package com.holisun.backend.controller;

import com.holisun.backend.dto.NotificationResponse;
import com.holisun.backend.entity.Notification;
import com.holisun.backend.enums.NotificationStatus;
import com.holisun.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationAdminController {

    private final NotificationRepository notificationRepository;

    @GetMapping
    public Page<NotificationResponse> getNotifications(
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) UUID appointmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifications;

        if (status != null && appointmentId != null) {
            notifications = notificationRepository.findAllByStatusAndAppointmentIdOrderByCreatedAtDesc(status, appointmentId, pageable);
        } else if (status != null) {
            notifications = notificationRepository.findAllByStatusOrderByCreatedAtDesc(status, pageable);
        } else if (appointmentId != null) {
            notifications = notificationRepository.findAllByAppointmentIdOrderByCreatedAtDesc(appointmentId, pageable);
        } else {
            notifications = notificationRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        return notifications.map(NotificationResponse::from);
    }

    @PostMapping("/{id}/retry")
    public void retryNotification(@PathVariable UUID id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notificarea nu a fost gasita"));

        if (notification.getStatus() != NotificationStatus.FAILED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Doar notificarile FAILED pot fi reincercate");
        }

        notification.setStatus(NotificationStatus.PENDING);
        notification.setAttempts(0);
        notification.setNextAttemptAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }
}
