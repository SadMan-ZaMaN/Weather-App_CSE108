package com.weatherfx;

import com.weatherfx.api.WeatherApiClient;
import com.weatherfx.comments.CommentsClient;
import com.weatherfx.model.Units;
import com.weatherfx.ui.AppContext;
import com.weatherfx.ui.Navigator;
import com.weatherfx.ui.WeatherBackground;
import javafx.application.Application;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.util.prefs.Preferences;

public class WeatherApp extends Application {

    private WeatherBackground background;

    @Override
    public void start(Stage stage) {
        AppConfig config = AppConfig.load();
        Preferences prefs = Preferences.userNodeForPackage(WeatherApp.class);

        ObjectProperty<Units> units = new SimpleObjectProperty<>(Units.fromName(prefs.get("units", "METRIC")));
        units.addListener((obs, old, now) -> prefs.put("units", now.name()));

        background = new WeatherBackground();
        StackPane content = new StackPane();
        Navigator navigator = new Navigator(content);

        AppContext context = new AppContext(
                new WeatherApiClient(config.apiKey()),
                new CommentsClient(config.commentsHost(), config.commentsPort()),
                background,
                navigator,
                units,
                prefs);
        navigator.attach(context);
        navigator.showMain();

        Scene scene = new Scene(new StackPane(background, content), 1280, 800);
        scene.getStylesheets().add(WeatherApp.class.getResource("css/app.css").toExternalForm());

        stage.setTitle("WeatherFX");
        stage.setMinWidth(1040);
        stage.setMinHeight(720);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (background != null) {
            background.dispose();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
