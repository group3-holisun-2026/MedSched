package com.holisun.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

/**
 * Placeholder — owner real: P1 (documents/module2/backend_module2_tasks.md, sectiunea 1).
 * Folosit atat pentru editare normala cat si pentru completarea profilului
 * (cnp/dateOfBirth/email/allergies/medicalHistory raman optionale).
 */
public record PatientRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String phone,
        @Pattern(regexp = "\\d{13}", message = "CNP trebuie sa aiba exact 13 cifre") String cnp,
        @Past LocalDate dateOfBirth,
        @Email String email,
        String allergies,
        String medicalHistory
) {
}
