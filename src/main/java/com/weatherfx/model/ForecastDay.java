package com.weatherfx.model;

import java.time.LocalDate;
import java.util.List;

public record ForecastDay(
        LocalDate date,
        double minC,
        double maxC,
        int chanceOfRain,
        String condition,
        String iconUrl,
        String sunrise,
        String sunset,
        List<HourlyForecast> hours
) {
}
