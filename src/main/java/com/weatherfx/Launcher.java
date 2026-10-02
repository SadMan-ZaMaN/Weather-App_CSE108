package com.weatherfx;

import javafx.application.Application;

// separate main class so the app runs from an IDE without module path setup
public class Launcher {
    public static void main(String[] args) {
        Application.launch(WeatherApp.class, args);
    }
}
