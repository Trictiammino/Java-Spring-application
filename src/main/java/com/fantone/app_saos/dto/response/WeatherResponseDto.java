package com.fantone.app_saos.dto.response;

public record WeatherResponseDto(
        double temperature,
        String condition,
        String description,
        String city
) {}
