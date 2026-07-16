package com.holisun.backend.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Placeholder — owner real: P1 (documents/module2/backend_module2_tasks.md, sectiunea 1).
 * Creat aici doar ca sa compileze PatientController; P1 poate ajusta/inlocui liber.
 * Creare rapida de recepție: doar nume + prenume + telefon.
 */
public record PatientQuickCreateRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String phone
) {
}
