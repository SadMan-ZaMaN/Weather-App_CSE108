package com.weatherfx.ui;

import com.weatherfx.api.WeatherApiException;
import com.weatherfx.model.CurrentWeather;
import com.weatherfx.model.ForecastDay;
import com.weatherfx.model.Location;
import com.weatherfx.model.Sky;
import com.weatherfx.model.Units;
import com.weatherfx.model.WeatherReport;
import com.weatherfx.mood.Mood;
import com.weatherfx.mood.MoodRecommendationEngine;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainController {

    private static final String AUTO_IP = "auto:ip";
    private static final String LAST_QUERY = "lastQuery";
    private static final DateTimeFormatter API_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm");
    private static final DateTimeFormatter NICE_TIME = DateTimeFormatter.ofPattern("EEEE d MMMM, h:mm a", Locale.ENGLISH);

    @FXML private TextField cityField;
    @FXML private Button searchButton;
    @FXML private Button locationButton;
    @FXML private ToggleButton celsiusToggle;
    @FXML private ToggleButton fahrenheitToggle;

    @FXML private Label cityLabel;
    @FXML private Label countryLabel;
    @FXML private Label localTimeLabel;
    @FXML private Label tempLabel;
    @FXML private Label humidityLabel;
    @FXML private Label windLabel;
    @FXML private Label conditionLabel;
    @FXML private Label feelsLikeLabel;

    @FXML private HBox forecastStrip;
    @FXML private ChoiceBox<Mood> moodSelector;
    @FXML private ListView<String> recommendationsList;

    @FXML private ProgressIndicator spinner;
    @FXML private Label statusLabel;
    @FXML private Button forecastButton;

    private final AppContext ctx;
    private final Map<String, Image> iconCache = new HashMap<>();
    private WeatherReport report;
    private int latestRequest;

    public MainController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        moodSelector.getItems().setAll(Mood.values());
        moodSelector.setValue(Mood.HAPPY);
        moodSelector.valueProperty().addListener((obs, old, mood) -> updateRecommendations());
        recommendationsList.setPlaceholder(new Label("Search for a city to get some ideas"));

        ToggleGroup unitGroup = new ToggleGroup();
        celsiusToggle.setToggleGroup(unitGroup);
        fahrenheitToggle.setToggleGroup(unitGroup);
        (ctx.units().get() == Units.IMPERIAL ? fahrenheitToggle : celsiusToggle).setSelected(true);
        unitGroup.selectedToggleProperty().addListener((obs, old, selected) -> {
            if (selected == null) {
                old.setSelected(true); // clicking the active one again shouldn't leave nothing selected
                return;
            }
            ctx.units().set(selected == fahrenheitToggle ? Units.IMPERIAL : Units.METRIC);
        });
        ctx.units().addListener((obs, old, units) -> render());

        forecastButton.setDisable(true);
        clearCards();

        if (!ctx.weatherApi().hasKey()) {
            cityLabel.setText("Almost there");
            countryLabel.setText("Add your free WeatherAPI key to config.properties and restart");
            showError("No API key found. Copy config.example.properties to config.properties and paste your key in.");
            return;
        }

        cityLabel.setText("WeatherFX");
        countryLabel.setText("Search for a city, or hit \"My location\"");
        load(ctx.prefs().get(LAST_QUERY, AUTO_IP));
    }

    @FXML
    private void searchCity() {
        String city = cityField.getText().trim();
        if (city.isEmpty()) {
            showError("Type a city name first.");
            cityField.requestFocus();
            return;
        }
        load(city);
    }

    @FXML
    private void showCurrentWeather() {
        cityField.clear();
        load(AUTO_IP);
    }

    @FXML
    private void showForecast() {
        if (report == null) {
            return;
        }
        ForecastController forecast = ctx.navigator().show("forecast-view.fxml");
        forecast.open(report.location(), placeName(report.location()));
    }

    @FXML
    private void showComments() {
        CommentsController comments = ctx.navigator().show("comments-view.fxml");
        if (report != null) {
            comments.setCurrentLocation(placeName(report.location()));
        }
    }

    @FXML
    private void exitApp() {
        Platform.exit();
    }

    private void load(String query) {
        int request = ++latestRequest;
        setBusy(true);
        showStatus(AUTO_IP.equals(query) ? "Finding where you are..." : "Looking up " + query + "...");

        Async.run(() -> ctx.weatherApi().fetchReport(query),
                result -> {
                    if (request != latestRequest) {
                        return; // a newer search has started since, ignore this one
                    }
                    setBusy(false);
                    report = result;
                    ctx.prefs().put(LAST_QUERY, query);

                    CurrentWeather now = result.current();
                    ctx.background().show(Sky.fromCondition(now.condition()), now.day());
                    forecastButton.setDisable(false);
                    showStatus("Last updated " + timePart(now.lastUpdated()) + " local time · data from WeatherAPI.com");
                    render();
                },
                error -> {
                    if (request != latestRequest) {
                        return;
                    }
                    setBusy(false);
                    showError(error instanceof WeatherApiException
                            ? error.getMessage()
                            : "Something went wrong: " + error);
                });
    }

    private void render() {
        if (report == null) {
            return;
        }
        Units units = ctx.units().get();
        Location location = report.location();
        CurrentWeather now = report.current();

        cityLabel.setText(location.name());
        countryLabel.setText(describePlace(location));
        localTimeLabel.setText(niceLocalTime(location.localTime()));

        tempLabel.setText(units.formatTemperature(now.tempC()));
        humidityLabel.setText(now.humidity() + "%");
        windLabel.setText(units.formatSpeed(now.windKph()));
        conditionLabel.setText(now.condition());
        feelsLikeLabel.setText(units.formatTemperature(now.feelsLikeC()));

        renderForecastStrip(units);
        updateRecommendations();
    }

    private void renderForecastStrip(Units units) {
        forecastStrip.getChildren().clear();
        List<ForecastDay> days = report.days();
        for (int i = 0; i < days.size(); i++) {
            ForecastDay day = days.get(i);

            Label name = new Label(i == 0 ? "Today" : day.date().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH));
            name.getStyleClass().add("day-name");

            ImageView icon = new ImageView();
            icon.setFitWidth(44);
            icon.setFitHeight(44);
            if (!day.iconUrl().isBlank()) {
                icon.setImage(iconCache.computeIfAbsent(day.iconUrl(), url -> new Image(url, 64, 64, true, true, true)));
            }

            Label temps = new Label(units.formatDegrees(day.maxC()) + " / " + units.formatDegrees(day.minC()));
            temps.getStyleClass().add("day-temps");

            Label condition = new Label(day.condition());
            condition.getStyleClass().add("day-detail");

            Label rain = new Label("Rain " + day.chanceOfRain() + "%");
            rain.getStyleClass().add("day-detail");

            VBox tile = new VBox(2, name, icon, temps, condition, rain);
            tile.getStyleClass().add("day-tile");
            tile.setAlignment(Pos.CENTER);
            tile.setMaxWidth(Double.MAX_VALUE);
            tile.setPrefWidth(0); // so all three tiles share the width evenly
            HBox.setHgrow(tile, Priority.ALWAYS);
            forecastStrip.getChildren().add(tile);
        }
    }

    private void updateRecommendations() {
        if (report == null) {
            recommendationsList.getItems().clear();
            return;
        }
        Sky sky = Sky.fromCondition(report.current().condition());
        recommendationsList.getItems().setAll(MoodRecommendationEngine.getRecommendations(sky, moodSelector.getValue()));
    }

    private void clearCards() {
        for (Label label : List.of(tempLabel, humidityLabel, windLabel, conditionLabel, feelsLikeLabel)) {
            label.setText("--");
        }
        Label hint = new Label("The next few days show up here after a search");
        hint.getStyleClass().add("hint");
        forecastStrip.getChildren().setAll(hint);
    }

    private void setBusy(boolean busy) {
        spinner.setVisible(busy);
        searchButton.setDisable(busy);
        locationButton.setDisable(busy);
    }

    private void showStatus(String message) {
        statusLabel.getStyleClass().remove("error");
        statusLabel.setText(message);
    }

    private void showError(String message) {
        if (!statusLabel.getStyleClass().contains("error")) {
            statusLabel.getStyleClass().add("error");
        }
        statusLabel.setText(message);
    }

    private static String countryName(Location location) {
        if ("Israel".equalsIgnoreCase(location.country())) {
            return "Occupied Palestine";
        }
        return location.country();
    }

    private static String placeName(Location location) {
        return location.name() + ", " + countryName(location);
    }

    private static String describePlace(Location location) {
        String region = location.region();
        if (region.isBlank() || region.equalsIgnoreCase(location.name())) {
            return countryName(location);
        }
        return region + ", " + countryName(location);
    }

    private static String niceLocalTime(String apiTime) {
        try {
            return NICE_TIME.format(LocalDateTime.parse(apiTime, API_TIME)) + " local time";
        } catch (DateTimeParseException e) {
            return "Local time: " + apiTime;
        }
    }

    // "2026-10-03 14:30" -> "14:30"
    private static String timePart(String apiTime) {
        int space = apiTime.indexOf(' ');
        return space < 0 ? apiTime : apiTime.substring(space + 1);
    }
}
