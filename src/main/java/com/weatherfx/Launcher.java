package com.weatherfx;

import javafx.application.Application;

/**
 * Plain main class so the app can be started straight from an IDE without
 * fiddling with module-path settings. ({@code mvn javafx:run} doesn't need it.)
 */
public class Launcher {
    public static void main(String[] args) {
        Application.launch(WeatherApp.class, args);
    }
}
