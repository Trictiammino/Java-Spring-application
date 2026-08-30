package com.fantone.app_saos.dto.response;

import java.util.List;

public record WeeklyWeatherResponseDto(
        String city,
        List<DailyForecastDto> forecast
) {}