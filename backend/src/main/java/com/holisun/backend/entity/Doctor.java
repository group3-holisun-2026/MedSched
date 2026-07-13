package com.holisun.backend.entity;

import com.holisun.backend.embeddables.WorkingHours;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name="doctors")
@Getter
@Setter
@NoArgsConstructor
public class Doctor {
    @Id
    @GeneratedValue(strategy= GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @Column(name = "speciality", nullable = false, length = 100)
    private String speciality;

    @Column(name = "contact_info", length = 255)
    private String contactInfo;

    @Column(name = "standard_consultation_duration_minutes", nullable = false)
    private Integer standardConsultationDurationMinutes;

    @ElementCollection
    private List<WorkingHours> weeklySchedule;
}