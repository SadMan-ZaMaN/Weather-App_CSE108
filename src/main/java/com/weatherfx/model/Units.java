package com.weatherfx.model;

public enum Units {
    METRIC("°C", "km/h"),
    IMPERIAL("°F", "mph");

    private final String temperatureSymbol;
    private final String speedSymbol;

    Units(String temperatureSymbol, String speedSymbol) {
        this.temperatureSymbol = temperatureSymbol;
        this.speedSymbol = speedSymbol;
    }

    public static Units fromName(String name) {
        for (Units units : values()) {
            if (units.name().equalsIgnoreCase(name)) {
                return units;
            }
        }
        return METRIC;
    }

    public double temperature(double celsius) {
        return this == METRIC ? celsius : celsius * 9 / 5 + 32;
    }

    public double speed(double kph) {
        return this == METRIC ? kph : kph / 1.609344;
    }

    public String formatTemperature(double celsius) {
        return Math.round(temperature(celsius)) + temperatureSymbol;
    }

    public String formatDegrees(double celsius) {
        return Math.round(temperature(celsius)) + "°";
    }

    public String formatSpeed(double kph) {
        return Math.round(speed(kph)) + " " + speedSymbol;
    }

    public String temperatureSymbol() {
        return temperatureSymbol;
    }

    public String speedSymbol() {
        return speedSymbol;
    }
}
