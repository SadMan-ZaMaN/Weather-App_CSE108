package com.weatherfx.model;

public record Location(String name, String region, String country, double lat, double lon, String localTime) {

    // "lat,lon" works for every endpoint and avoids picking the wrong Springfield
    public String coordinates() {
        return lat + "," + lon;
    }
}
