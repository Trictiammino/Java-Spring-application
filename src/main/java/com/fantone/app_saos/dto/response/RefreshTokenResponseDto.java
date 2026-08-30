package com.fantone.app_saos.dto.response;

public record RefreshTokenResponseDto(
        String accessToken,
        String tokenType,
        long accessExpires
) {}
