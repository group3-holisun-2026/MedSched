package com.holisun.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "equipment_types")
@Getter
@Setter
@NoArgsConstructor
public class EquipmentType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name; // e.g. "4D Doppler Ultrasound"

    @Column(name = "description", length = 255)
    private String description;
}