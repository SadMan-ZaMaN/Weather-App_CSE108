package com.weatherfx.ui;

import com.weatherfx.api.WeatherApiClient;
import com.weatherfx.comments.CommentsClient;
import com.weatherfx.model.Units;
import javafx.beans.property.ObjectProperty;

import java.util.prefs.Preferences;

public record AppContext(
        WeatherApiClient weatherApi,
        CommentsClient comments,
        WeatherBackground background,
        Navigator navigator,
        ObjectProperty<Units> units,
        Preferences prefs
) {
}
