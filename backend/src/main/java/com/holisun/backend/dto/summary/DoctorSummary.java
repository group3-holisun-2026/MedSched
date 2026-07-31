package com.holisun.backend.dto.summary;

import java.util.UUID;

/**
 * `userId` e id-ul contului (User), nu al medicului — frontend-ul il compara cu
 * `user.id` din AuthContext ca sa stie daca programarea afisata e a medicului logat
 * (vezi documents/module4/frontend_module4_tasks.md, sectiunea 1).
 */
public record DoctorSummary(UUID id, UUID userId, String username, String speciality) {}
