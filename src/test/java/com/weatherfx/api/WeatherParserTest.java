package com.weatherfx.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.weatherfx.model.CurrentWeather;
import com.weatherfx.model.ForecastDay;
import com.weatherfx.model.HourlyForecast;
import com.weatherfx.model.Location;
import com.weatherfx.model.WeatherReport;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The JSON files are real WeatherAPI responses, trimmed down to a few hours per day. */
class WeatherParserTest {

    @Test
    void parsesForecastResponse() throws IOException {
        WeatherReport report = WeatherParser.parseReport(load("forecast-dhaka.json"));

        Location location = report.location();
        assertEquals("Dhaka", location.name());
        assertEquals("Bangladesh", location.country());
        assertEquals("23.7231,90.4086", location.coordinates());

        CurrentWeather now = report.current();
        assertEquals(27.1, now.tempC(), 0.001);
        assertEquals(71, now.humidity());
        assertEquals("Patchy rain nearby", now.condition());
        assertFalse(now.day());
        assertTrue(now.iconUrl().startsWith("https://cdn.weatherapi.com/"), now.iconUrl());

        assertEquals(3, report.days().size());
        ForecastDay today = report.days().get(0);
        assertEquals(LocalDate.of(2026, 10, 3), today.date());
        assertTrue(today.minC() <= today.maxC());
        assertFalse(today.sunrise().isBlank());
    }

    @Test
    void parsesHourlyDataFromHistoryResponse() throws IOException {
        List<ForecastDay> days = WeatherParser.parseDays(load("history-london.json"));

        assertEquals(1, days.size());
        List<HourlyForecast> hours = days.get(0).hours();
        assertEquals(4, hours.size());
        assertEquals(LocalTime.MIDNIGHT, hours.get(0).time());
        assertEquals(LocalTime.of(6, 0), hours.get(1).time());
    }

    @Test
    void responseWithoutForecastGivesNoDays() {
        JsonObject json = JsonParser.parseString("{\"location\": {}}").getAsJsonObject();
        assertTrue(WeatherParser.parseDays(json).isEmpty());
    }

    @Test
    void knownErrorCodesGetFriendlierMessages() {
        assertEquals("Couldn't find that place. Check the spelling and try again.",
                WeatherApiClient.friendlyMessage(1006, "No matching location found."));
        assertEquals("Something new", WeatherApiClient.friendlyMessage(9999, "Something new"));
    }

    private JsonObject load(String name) throws IOException {
        try (InputStream in = WeatherParserTest.class.getResourceAsStream(name)) {
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return JsonParser.parseString(json).getAsJsonObject();
        }
    }
}
