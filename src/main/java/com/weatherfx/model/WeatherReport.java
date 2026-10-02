package com.weatherfx.model;

import java.util.List;

public record WeatherReport(Location location, CurrentWeather current, List<ForecastDay> days) {
}
