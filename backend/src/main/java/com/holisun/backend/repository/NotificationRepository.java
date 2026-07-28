package com.holisun.backend.repository;

import com.holisun.backend.entity.Notification;
import com.holisun.backend.enums.NotificationStatus;
import com.holisun.backend.enums.NotificationTrigger;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    public List<Notification> findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(NotificationStatus status, LocalDateTime now);

    public Optional<Notification> findByConfirmationToken(String token);

    public List<Notification> findByAppointmentIdAndTriggerAndStatus(UUID appointmentId, NotificationTrigger trigger, NotificationStatus status);

    public Page<Notification> findAllByOrderByCreatedAtDesc(Pageable pageable);

    public Page<Notification> findAllByStatusOrderByCreatedAtDesc(NotificationStatus status, Pageable pageable);
}
