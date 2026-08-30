package com.fantone.app_saos.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CardDto (
        Long id,
        BigDecimal balance,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {}
