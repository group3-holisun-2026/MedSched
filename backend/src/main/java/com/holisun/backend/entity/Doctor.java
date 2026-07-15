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
    @JoinColumn(name = "user_id", referencedColumnName = "id", unique = true)
    private User user;

    @Column(name = "specialization", nullable = false, length = 150)
    private String specialization;

    @Column(name = "standard_appointment_duration_minutes", nullable = false)
    private Integer standardAppointmentDurationMinutes;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @ElementCollection
    @CollectionTable(
            name = "doctor_weekly_schedule",
            joinColumns = @JoinColumn(name = "doctor_id")
    )
    private List<WorkingHours> weeklySchedule;
}
