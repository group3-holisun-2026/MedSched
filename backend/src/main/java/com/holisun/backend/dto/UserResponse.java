package com.holisun.backend.dto;

import com.holisun.backend.enums.Role;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        String phone,
        String city,
        Role role
) {}
