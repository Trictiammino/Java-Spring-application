package com.fantone.app_saos.dto.response;

public record TokenJWTResponseDto(
            String accessToken,
            String refreshToken,
            String tokenType,
            long accessExpires,
            long refreshExpires
    ) {}
