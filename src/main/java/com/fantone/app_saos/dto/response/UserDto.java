package com.fantone.app_saos.dto.response;

import java.time.LocalDateTime;

public record UserDto(
        Long id,
        String username,
        String name,
        String lastname,
        String email,
        String address,
        int age,
        LocalDateTime createdAt
) {}
