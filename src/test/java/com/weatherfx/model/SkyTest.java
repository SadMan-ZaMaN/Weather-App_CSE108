package com.weatherfx.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkyTest {

    @ParameterizedTest
    @CsvSource({
            "Sunny, CLEAR",
            "Clear, CLEAR",
            "Partly Cloudy , CLOUDY",
            "Overcast, CLOUDY",
            "Mist, MIST",
            "Freezing fog, MIST",
            "Patchy rain nearby, LIGHT_RAIN",
            "Light drizzle, LIGHT_RAIN",
            "Light rain shower, LIGHT_RAIN",
            "Moderate rain at times, HEAVY_RAIN",
            "Torrential rain shower, HEAVY_RAIN",
            "Moderate or heavy freezing rain, HEAVY_RAIN",
            "Light snow, SNOW",
            "Moderate or heavy sleet, SNOW",
            "Ice pellets, SNOW",
            "Blizzard, SNOW",
            "Thundery outbreaks in nearby, THUNDER",
            "Patchy light rain with thunder, THUNDER",
            "Moderate or heavy snow with thunder, THUNDER",
    })
    void mapsWeatherApiConditions(String condition, Sky expected) {
        assertEquals(expected, Sky.fromCondition(condition));
    }

    @Test
    void unknownOrMissingConditionFallsBackToClear() {
        assertEquals(Sky.CLEAR, Sky.fromCondition(null));
        assertEquals(Sky.CLEAR, Sky.fromCondition("Something the API made up"));
    }

    @Test
    void clearSkyUsesDifferentVideoAtNight() {
        assertEquals("Sunny", Sky.CLEAR.videoName(true));
        assertEquals("Clear", Sky.CLEAR.videoName(false));
        assertEquals("Heavy_Rain", Sky.HEAVY_RAIN.videoName(true));
    }
}
