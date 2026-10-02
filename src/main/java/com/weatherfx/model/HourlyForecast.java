package com.weatherfx.model;

import java.time.LocalTime;

public record HourlyForecast(LocalTime time, double tempC, int humidity, int chanceOfRain, double windKph) {
}
