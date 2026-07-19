package com.holisun.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "consultation_records",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uc_consultation_records_appointment_id",
                        columnNames = "appointment_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ConsultationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "appointment_id", nullable = false)
    private UUID appointmentId;

    @Column(name = "presentation_motive", columnDefinition = "TEXT")
    private String presentationMotive;

    @Column(name = "anamnesis", columnDefinition = "TEXT")
    private String anamnesis;

    @Column(name = "clinical_exam", columnDefinition = "TEXT")
    private String clinicalExam;

    @Column(name = "diagnosis", columnDefinition = "TEXT")
    private String diagnosis;

    @Column(name = "prescription", columnDefinition = "TEXT")
    private String prescription;

    @Column(name = "locked", nullable = false)
    private boolean locked = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}