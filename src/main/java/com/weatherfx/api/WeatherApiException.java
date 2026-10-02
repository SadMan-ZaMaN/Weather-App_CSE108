package com.weatherfx.api;

/** Something went wrong talking to WeatherAPI. The message is meant to be shown to the user as-is. */
public class WeatherApiException extends Exception {

    public WeatherApiException(String message) {
        super(message);
    }

    public WeatherApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
