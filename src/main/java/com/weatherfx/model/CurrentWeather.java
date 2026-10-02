package com.weatherfx.model;

public record CurrentWeather(
        double tempC,
        double feelsLikeC,
        int humidity,
        double windKph,
        String windDir,
        String condition,
        String iconUrl,
        boolean day,
        String lastUpdated
) {
}
