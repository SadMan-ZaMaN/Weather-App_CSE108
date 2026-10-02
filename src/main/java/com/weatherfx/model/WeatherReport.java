package com.weatherfx.model;

import java.util.List;

/** Everything the main screen needs from one API call: where, what it's like now, and the next few days. */
public record WeatherReport(Location location, CurrentWeather current, List<ForecastDay> days) {
}
