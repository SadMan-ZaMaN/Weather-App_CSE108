package com.weatherfx.mood;

import com.weatherfx.model.Sky;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoodRecommendationEngineTest {

    @Test
    void everyWeatherAndMoodHasSomethingToSuggest() {
        for (Sky sky : Sky.values()) {
            for (Mood mood : Mood.values()) {
                List<String> tips = MoodRecommendationEngine.getRecommendations(sky, mood);
                assertFalse(tips.isEmpty(), sky + " / " + mood);
            }
        }
    }

    @Test
    void rainyDaysSuggestIndoorStuff() {
        List<String> tips = MoodRecommendationEngine.getRecommendations(Sky.LIGHT_RAIN, Mood.RELAXED);
        assertTrue(tips.contains("Make hot chocolate"));
    }

    @Test
    void missingInputStillReturnsAHint() {
        assertFalse(MoodRecommendationEngine.getRecommendations(null, Mood.HAPPY).isEmpty());
    }
}
