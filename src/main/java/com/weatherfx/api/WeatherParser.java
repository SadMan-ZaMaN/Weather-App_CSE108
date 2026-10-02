package com.weatherfx.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.weatherfx.model.CurrentWeather;
import com.weatherfx.model.ForecastDay;
import com.weatherfx.model.HourlyForecast;
import com.weatherfx.model.Location;
import com.weatherfx.model.WeatherReport;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public final class WeatherParser {

    private WeatherParser() {
    }

    public static WeatherReport parseReport(JsonObject json) {
        return new WeatherReport(
                parseLocation(json.getAsJsonObject("location")),
                parseCurrent(json.getAsJsonObject("current")),
                parseDays(json));
    }

    public static List<ForecastDay> parseDays(JsonObject json) {
        List<ForecastDay> days = new ArrayList<>();
        JsonObject forecast = json.getAsJsonObject("forecast");
        if (forecast == null) {
            return days;
        }
        for (JsonElement element : forecast.getAsJsonArray("forecastday")) {
            days.add(parseDay(element.getAsJsonObject()));
        }
        return days;
    }

    static Location parseLocation(JsonObject location) {
        return new Location(
                location.get("name").getAsString(),
                text(location, "region"),
                location.get("country").getAsString(),
                location.get("lat").getAsDouble(),
                location.get("lon").getAsDouble(),
                text(location, "localtime"));
    }

    static CurrentWeather parseCurrent(JsonObject current) {
        JsonObject condition = current.getAsJsonObject("condition");
        return new CurrentWeather(
                current.get("temp_c").getAsDouble(),
                current.get("feelslike_c").getAsDouble(),
                current.get("humidity").getAsInt(),
                current.get("wind_kph").getAsDouble(),
                text(current, "wind_dir"),
                condition.get("text").getAsString().trim(),
                iconUrl(condition),
                current.get("is_day").getAsInt() == 1,
                text(current, "last_updated"));
    }

    static ForecastDay parseDay(JsonObject forecastDay) {
        JsonObject day = forecastDay.getAsJsonObject("day");
        JsonObject astro = forecastDay.getAsJsonObject("astro");
        JsonObject condition = day.getAsJsonObject("condition");

        List<HourlyForecast> hours = new ArrayList<>();
        JsonArray hourArray = forecastDay.getAsJsonArray("hour");
        if (hourArray != null) {
            for (JsonElement element : hourArray) {
                hours.add(parseHour(element.getAsJsonObject()));
            }
        }

        return new ForecastDay(
                LocalDate.parse(forecastDay.get("date").getAsString()),
                day.get("mintemp_c").getAsDouble(),
                day.get("maxtemp_c").getAsDouble(),
                integer(day, "daily_chance_of_rain"),
                condition.get("text").getAsString().trim(),
                iconUrl(condition),
                text(astro, "sunrise"),
                text(astro, "sunset"),
                hours);
    }

    static HourlyForecast parseHour(JsonObject hour) {
        // "2026-10-03 14:00" -> 14:00
        String time = hour.get("time").getAsString();
        return new HourlyForecast(
                LocalTime.parse(time.substring(time.indexOf(' ') + 1)),
                hour.get("temp_c").getAsDouble(),
                hour.get("humidity").getAsInt(),
                integer(hour, "chance_of_rain"),
                hour.get("wind_kph").getAsDouble());
    }

    // icon urls come without https, like "//cdn.weatherapi.com/..."
    private static String iconUrl(JsonObject condition) {
        String icon = text(condition, "icon");
        return icon.startsWith("//") ? "https:" + icon : icon;
    }

    private static String text(JsonObject obj, String key) {
        JsonElement value = obj == null ? null : obj.get(key);
        return value == null || value.isJsonNull() ? "" : value.getAsString();
    }

    private static int integer(JsonObject obj, String key) {
        JsonElement value = obj.get(key);
        return value == null || value.isJsonNull() ? 0 : value.getAsInt();
    }
}
