package com.weatherfx;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Reads settings from {@code config.properties} in the folder the app is started from.
 * The API key can also come from the WEATHER_API_KEY environment variable, which wins if both are set.
 */
public final class AppConfig {

    public static final String FILE_NAME = "config.properties";

    private final Properties props;

    private AppConfig(Properties props) {
        this.props = props;
    }

    public static AppConfig load() {
        return load(Path.of(FILE_NAME));
    }

    static AppConfig load(Path file) {
        Properties props = new Properties();
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                props.load(reader);
            } catch (IOException e) {
                System.err.println("Couldn't read " + file + ": " + e.getMessage());
            }
        }
        return new AppConfig(props);
    }

    public String apiKey() {
        String fromEnv = System.getenv("WEATHER_API_KEY");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        return props.getProperty("weatherapi.key", "").trim();
    }

    public String commentsHost() {
        return props.getProperty("comments.host", "localhost").trim();
    }

    public int commentsPort() {
        return intProperty("comments.port", 5555);
    }

    private int intProperty(String key, int fallback) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            System.err.println("Ignoring bad value for " + key + ": " + value);
            return fallback;
        }
    }
}
