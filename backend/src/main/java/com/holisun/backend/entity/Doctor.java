package com.holisun.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "doctors")
@Getter
@Setter
@NoArgsConstructor
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, unique = true)
    private User user;

    @Column(name = "speciality", nullable = false, length = 150)
    private String speciality;

    @Column(name = "standard_consultation_duration_minutes", nullable = false)
    private Integer standardConsultationDurationMinutes;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @OneToMany(mappedBy = "doctor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkSchedule> weeklySchedule = new ArrayList<>();
}
