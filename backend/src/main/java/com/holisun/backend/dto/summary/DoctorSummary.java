package com.holisun.backend.dto.summary;

import java.util.UUID;

public record DoctorSummary(UUID id, String firstName, String lastName, String speciality) {}