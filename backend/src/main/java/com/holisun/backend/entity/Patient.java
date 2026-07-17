package com.holisun.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "patients")
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, name = "phone", length = 20)
    private String phone;

    @Convert()
    @Column(name = "cnp", nullable = true, length = 512)
    private String cnp;

    @Column(name = "cnp_hash", nullable = true, unique = true, length = 64)
    private String cnpHash;

    @Column(name = "date_of_birth", nullable = true)
    private LocalDate dateOfBirth;

    @Column(name = "email", nullable = true, length = 255)
    private String email;

    @Column(name = "allergies", nullable = true, columnDefinition = "TEXT")
    private String allergies; // e.g work

    @Column(name = "medical_history", nullable = true, columnDefinition = "TEXT")
    private String medicalHistory;

    @Column(name = "profile_complete", nullable = false)
    private Boolean profileComplete = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;


    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PrePersist
    @PreUpdate
    private void updateProfileComplete() {
        profileComplete =
                cnp != null && dateOfBirth != null;
    }


}
