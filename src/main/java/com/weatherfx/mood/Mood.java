package com.weatherfx.mood;

public enum Mood {
    HAPPY("Happy"),
    SAD("Sad"),
    ENERGETIC("Energetic"),
    RELAXED("Relaxed");

    private final String label;

    Mood(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
