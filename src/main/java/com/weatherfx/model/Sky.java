package com.weatherfx.model;

import java.util.Locale;

/**
 * The handful of "looks" the app knows how to draw. WeatherAPI has ~50 condition texts,
 * so this squashes them down into something the background and the mood tips can work with.
 */
public enum Sky {
    CLEAR,
    CLOUDY,
    LIGHT_RAIN,
    HEAVY_RAIN,
    SNOW,
    MIST,
    THUNDER;

    public static Sky fromCondition(String condition) {
        if (condition == null) {
            return CLEAR;
        }
        String text = condition.toLowerCase(Locale.ROOT);

        // order matters: "Patchy light rain with thunder" should be a storm, not light rain
        if (text.contains("thunder")) {
            return THUNDER;
        }
        if (text.contains("snow") || text.contains("sleet") || text.contains("ice") || text.contains("blizzard")) {
            return SNOW;
        }
        if (text.contains("heavy") || text.contains("torrential") || text.contains("moderate rain")) {
            return HEAVY_RAIN;
        }
        if (text.contains("rain") || text.contains("drizzle") || text.contains("shower")) {
            return LIGHT_RAIN;
        }
        if (text.contains("mist") || text.contains("fog") || text.contains("haze")) {
            return MIST;
        }
        if (text.contains("cloud") || text.contains("overcast")) {
            return CLOUDY;
        }
        return CLEAR;
    }

    /** File name (without .mp4) of the matching background clip from the original version of the app. */
    public String videoName(boolean day) {
        return switch (this) {
            case CLEAR -> day ? "Sunny" : "Clear";
            case CLOUDY -> "Cloudy";
            case LIGHT_RAIN -> "Light_Rain";
            case HEAVY_RAIN -> "Heavy_Rain";
            case SNOW -> "Snow";
            case MIST -> "Mist";
            case THUNDER -> "Thunder";
        };
    }
}
