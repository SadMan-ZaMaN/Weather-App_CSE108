package com.weatherfx.ui;

import com.weatherfx.api.WeatherApiClient;
import com.weatherfx.api.WeatherApiException;
import com.weatherfx.model.ForecastDay;
import com.weatherfx.model.HourlyForecast;
import com.weatherfx.model.Location;
import com.weatherfx.model.Units;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;

import java.time.LocalDate;
import java.util.Locale;

public class ForecastController {

    private enum Metric { TEMPERATURE, HUMIDITY, RAIN, WIND }

    @FXML private Label placeLabel;
    @FXML private DatePicker datePicker;
    @FXML private ToggleButton temperatureToggle;
    @FXML private ToggleButton humidityToggle;
    @FXML private ToggleButton rainToggle;
    @FXML private ToggleButton windToggle;
    @FXML private LineChart<String, Number> chart;
    @FXML private NumberAxis yAxis;
    @FXML private ProgressIndicator spinner;
    @FXML private Label messageLabel;
    @FXML private Label sunriseLabel;
    @FXML private Label sunsetLabel;
    @FXML private Label summaryLabel;

    private final AppContext ctx;
    private Location location;
    private ForecastDay day;
    private Metric metric = Metric.TEMPERATURE;
    private int latestRequest;

    public ForecastController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        ToggleGroup metrics = new ToggleGroup();
        temperatureToggle.setUserData(Metric.TEMPERATURE);
        humidityToggle.setUserData(Metric.HUMIDITY);
        rainToggle.setUserData(Metric.RAIN);
        windToggle.setUserData(Metric.WIND);
        for (ToggleButton toggle : new ToggleButton[]{temperatureToggle, humidityToggle, rainToggle, windToggle}) {
            toggle.setToggleGroup(metrics);
        }
        temperatureToggle.setSelected(true);
        metrics.selectedToggleProperty().addListener((obs, old, selected) -> {
            if (selected == null) {
                old.setSelected(true);
                return;
            }
            metric = (Metric) selected.getUserData();
            drawChart();
        });

        // only offer days the API will actually give us
        LocalDate today = LocalDate.now();
        LocalDate first = today.minusDays(WeatherApiClient.HISTORY_DAYS);
        LocalDate last = today.plusDays(WeatherApiClient.FORECAST_DAYS);
        datePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(first) || date.isAfter(last));
            }
        });
        datePicker.setValue(today);
        datePicker.valueProperty().addListener((obs, old, date) -> load());

        clearSummary();
    }

    public void open(Location location, String placeName) {
        this.location = location;
        placeLabel.setText(placeName);
        load();
    }

    @FXML
    private void goBack() {
        ctx.navigator().showMain();
    }

    private void load() {
        LocalDate date = datePicker.getValue();
        if (location == null || date == null) {
            return;
        }
        int request = ++latestRequest;
        spinner.setVisible(true);
        messageLabel.setVisible(false);

        Async.run(() -> ctx.weatherApi().fetchDay(location, date),
                result -> {
                    if (request != latestRequest) {
                        return;
                    }
                    spinner.setVisible(false);
                    day = result;
                    showSummary();
                    drawChart();
                },
                error -> {
                    if (request != latestRequest) {
                        return;
                    }
                    spinner.setVisible(false);
                    day = null;
                    chart.getData().clear();
                    clearSummary();
                    showMessage(error instanceof WeatherApiException
                            ? error.getMessage()
                            : "Couldn't load the forecast: " + error);
                });
    }

    private void showSummary() {
        Units units = ctx.units().get();
        sunriseLabel.setText("Sunrise  " + day.sunrise());
        sunsetLabel.setText("Sunset  " + day.sunset());
        summaryLabel.setText("High " + units.formatTemperature(day.maxC())
                + "  ·  Low " + units.formatTemperature(day.minC())
                + "  ·  " + day.condition());
    }

    private void clearSummary() {
        sunriseLabel.setText("Sunrise  --");
        sunsetLabel.setText("Sunset  --");
        summaryLabel.setText("--");
    }

    private void drawChart() {
        if (day == null) {
            return;
        }
        if (day.hours().isEmpty()) {
            chart.getData().clear();
            showMessage("No hourly data for this day.");
            return;
        }
        messageLabel.setVisible(false);

        Units units = ctx.units().get();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (HourlyForecast hour : day.hours()) {
            series.getData().add(new XYChart.Data<>(hour.time().toString(), valueOf(hour, units)));
        }

        boolean percentage = metric == Metric.HUMIDITY || metric == Metric.RAIN;
        yAxis.setAutoRanging(!percentage);
        if (percentage) {
            yAxis.setLowerBound(0);
            yAxis.setUpperBound(100);
            yAxis.setTickUnit(20);
        }
        yAxis.setLabel(axisLabel(units));

        // the line colour comes from app.css, one class per metric
        chart.getStyleClass().removeIf(styleClass -> styleClass.startsWith("metric-"));
        chart.getStyleClass().add("metric-" + metric.name().toLowerCase(Locale.ROOT));
        chart.getData().clear();
        chart.getData().add(series);

        String unit = unitSuffix(units);
        for (XYChart.Data<String, Number> point : series.getData()) {
            Tooltip tooltip = new Tooltip(point.getXValue() + "   " + Math.round(point.getYValue().doubleValue()) + unit);
            tooltip.setShowDelay(Duration.millis(60));
            Tooltip.install(point.getNode(), tooltip);
        }
    }

    private double valueOf(HourlyForecast hour, Units units) {
        return switch (metric) {
            case TEMPERATURE -> units.temperature(hour.tempC());
            case HUMIDITY -> hour.humidity();
            case RAIN -> hour.chanceOfRain();
            case WIND -> units.speed(hour.windKph());
        };
    }

    private String axisLabel(Units units) {
        return switch (metric) {
            case TEMPERATURE -> "Temperature (" + units.temperatureSymbol() + ")";
            case HUMIDITY -> "Humidity (%)";
            case RAIN -> "Chance of rain (%)";
            case WIND -> "Wind (" + units.speedSymbol() + ")";
        };
    }

    private String unitSuffix(Units units) {
        return switch (metric) {
            case TEMPERATURE -> units.temperatureSymbol();
            case HUMIDITY, RAIN -> "%";
            case WIND -> " " + units.speedSymbol();
        };
    }

    private void showMessage(String message) {
        messageLabel.setText(message);
        messageLabel.setVisible(true);
    }
}
