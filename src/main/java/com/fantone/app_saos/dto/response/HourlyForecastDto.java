package com.fantone.app_saos.dto.response;

public record HourlyForecastDto(
        String time,
        double temperature,
        String condition,
        String description
) {}