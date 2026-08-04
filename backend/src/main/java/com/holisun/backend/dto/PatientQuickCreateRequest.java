package com.holisun.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Placeholder — owner real: P1 (documents/module2/backend_module2_tasks.md, sectiunea 1).
 * Creat aici doar ca sa compileze PatientController; P1 poate ajusta/inlocui liber.
 * Creare rapida de recepție: nume + prenume + telefon, plus emailul, optional.
 *
 * Emailul e singurul camp din profilul complet care nu poate astepta completarea ulterioara:
 * confirmarea (F-501) se pune in coada in aceeasi tranzactie cu programarea, deci un pacient
 * creat fara adresa primea direct o notificare FAILED ("Invalid email address: null"), pe care
 * nici completarea profilului de mai tarziu nu o mai repara.
 */
public record PatientQuickCreateRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String phone,
        @Email String email
) {
}
