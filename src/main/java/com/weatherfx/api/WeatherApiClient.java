package com.weatherfx.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.weatherfx.model.ForecastDay;
import com.weatherfx.model.Location;
import com.weatherfx.model.WeatherReport;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class WeatherApiClient {

    private static final String BASE_URL = "https://api.weatherapi.com/v1/";

    // free plan limits
    public static final int HISTORY_DAYS = 7;
    public static final int FORECAST_DAYS = 14;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();
    private final String apiKey;

    public WeatherApiClient(String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    public boolean hasKey() {
        return !apiKey.isEmpty();
    }

    // query can be a city name, "lat,lon" or "auto:ip"
    public WeatherReport fetchReport(String query) throws WeatherApiException {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("q", query);
        params.put("days", "3");
        params.put("aqi", "no");
        params.put("alerts", "no");
        return parse(get("forecast.json", params), WeatherParser::parseReport);
    }

    public ForecastDay fetchDay(Location location, LocalDate date) throws WeatherApiException {
        String endpoint = date.isBefore(LocalDate.now()) ? "history.json" : "forecast.json";
        Map<String, String> params = new LinkedHashMap<>();
        params.put("q", location.coordinates());
        params.put("dt", date.toString());

        List<ForecastDay> days = parse(get(endpoint, params), WeatherParser::parseDays);
        if (days.isEmpty()) {
            throw new WeatherApiException("WeatherAPI has no data for " + date + ". Try a date closer to today.");
        }
        return days.get(0);
    }

    private JsonObject get(String endpoint, Map<String, String> params) throws WeatherApiException {
        if (!hasKey()) {
            throw new WeatherApiException("No WeatherAPI key found. Put yours in config.properties (see the README).");
        }

        StringBuilder url = new StringBuilder(BASE_URL).append(endpoint).append("?key=").append(encode(apiKey));
        params.forEach((name, value) -> url.append('&').append(name).append('=').append(encode(value)));

        HttpRequest request = HttpRequest.newBuilder(URI.create(url.toString()))
                .timeout(Duration.ofSeconds(12))
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException e) {
            throw new WeatherApiException("WeatherAPI took too long to answer. Try again in a bit.", e);
        } catch (IOException e) {
            throw new WeatherApiException("Couldn't reach WeatherAPI. Are you connected to the internet?", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WeatherApiException("The request was interrupted.", e);
        }

        JsonObject json;
        try {
            JsonElement body = JsonParser.parseString(response.body());
            if (!body.isJsonObject()) {
                throw new JsonParseException("not an object");
            }
            json = body.getAsJsonObject();
        } catch (JsonParseException e) {
            throw new WeatherApiException("WeatherAPI sent back something unexpected (HTTP " + response.statusCode() + ").", e);
        }

        if (json.has("error")) {
            JsonObject error = json.getAsJsonObject("error");
            throw new WeatherApiException(friendlyMessage(error.get("code").getAsInt(), error.get("message").getAsString()));
        }
        if (response.statusCode() != 200) {
            throw new WeatherApiException("WeatherAPI returned HTTP " + response.statusCode() + ".");
        }
        return json;
    }

    private static <T> T parse(JsonObject json, Function<JsonObject, T> parser) throws WeatherApiException {
        try {
            return parser.apply(json);
        } catch (RuntimeException e) {
            throw new WeatherApiException("WeatherAPI sent back data in a shape I didn't expect.", e);
        }
    }

    // error codes: https://www.weatherapi.com/docs/#intro-error-codes
    static String friendlyMessage(int code, String apiMessage) {
        return switch (code) {
            case 1006 -> "Couldn't find that place. Check the spelling and try again.";
            case 1002, 2006 -> "WeatherAPI didn't accept the API key. Double-check it in config.properties.";
            case 2007 -> "This API key has used up its monthly quota.";
            case 2008 -> "This API key has been disabled. You can get a new one for free at weatherapi.com.";
            default -> apiMessage;
        };
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
