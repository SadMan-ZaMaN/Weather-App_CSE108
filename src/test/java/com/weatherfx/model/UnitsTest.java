package com.weatherfx.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UnitsTest {

    @Test
    void convertsTemperature() {
        assertEquals(32, Units.IMPERIAL.temperature(0), 0.001);
        assertEquals(212, Units.IMPERIAL.temperature(100), 0.001);
        assertEquals(-40, Units.IMPERIAL.temperature(-40), 0.001);
        assertEquals(21.5, Units.METRIC.temperature(21.5), 0.001);
    }

    @Test
    void convertsSpeed() {
        assertEquals(10, Units.IMPERIAL.speed(16.09344), 0.001);
        assertEquals(16, Units.METRIC.speed(16), 0.001);
    }

    @Test
    void formatsForDisplay() {
        assertEquals("27°C", Units.METRIC.formatTemperature(27.1));
        assertEquals("81°F", Units.IMPERIAL.formatTemperature(27.1));
        assertEquals("9 km/h", Units.METRIC.formatSpeed(9.0));
        assertEquals("6 mph", Units.IMPERIAL.formatSpeed(9.0));
        assertEquals("31°", Units.METRIC.formatDegrees(30.6));
    }

    @Test
    void readsSavedPreference() {
        assertEquals(Units.IMPERIAL, Units.fromName("imperial"));
        assertEquals(Units.METRIC, Units.fromName("nonsense"));
        assertEquals(Units.METRIC, Units.fromName(null));
    }
}
