package com.fantone.app_saos.service.payload;

public record AuthTokens(
        String accessToken,
        String refreshToken
) {}
