package com.fantone.app_saos.dto.response;

import java.time.LocalDateTime;

public record MembershipResponseDto(
        Long id,
        String planName,
        String status,
        LocalDateTime expiresAt
) {}