package com.fantone.app_saos.dto.response;

import java.util.List;

public record DailyForecastDto(
        String date,
        List<HourlyForecastDto> hourlyForecasts
) {}