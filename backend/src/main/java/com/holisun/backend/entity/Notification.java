package com.holisun.backend.entity;

import com.holisun.backend.enums.NotificationStatus;
import com.holisun.backend.enums.NotificationTrigger;
import com.holisun.backend.enums.NotificationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "notifications")
@Setter
@Getter
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(nullable = false)
    private Appointment appointment;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private NotificationType Type;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger")
    private NotificationTrigger trigger;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private NotificationStatus status =  NotificationStatus.PENDING;

    @Column(name = "recipient_phone", length = 20)
    private String recipientPhone;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt;

    @Column(name = "attempts")
    private int attempts = 0;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "provider_message_id", length = 64)
    private String providerMessageId;

    @Column(name = "confirmation_token")
    private String confirmationToken;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
