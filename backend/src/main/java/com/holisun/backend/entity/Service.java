package com.holisun.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "services")
@Getter
@Setter
@NoArgsConstructor
public class Service {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false, unique = true, length = 150)
    private String name;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "default_duration_minutes", nullable = false)
    private Integer defaultDurationMinutes;

    @ManyToMany
    @JoinTable(
            name = "service_required_equipment_types",
            joinColumns = @JoinColumn(name = "service_id"),
            inverseJoinColumns = @JoinColumn(name = "equipment_type_id")
    )
    private Set<EquipmentType> requiredEquipmentTypes = new HashSet<>();

    @Column(name = "active", nullable = false)
    private Boolean active = true;
}