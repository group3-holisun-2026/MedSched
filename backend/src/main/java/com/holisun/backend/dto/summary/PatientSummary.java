package com.holisun.backend.dto.summary;

import java.util.UUID;

public record PatientSummary(UUID id, String firstName, String lastName) {}
