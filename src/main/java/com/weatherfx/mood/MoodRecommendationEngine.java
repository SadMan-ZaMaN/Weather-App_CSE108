package com.weatherfx.mood;

import com.weatherfx.model.Sky;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class MoodRecommendationEngine {

    private static final Map<Sky, Map<Mood, List<String>>> RECOMMENDATIONS = new EnumMap<>(Sky.class);

    static {
        Map<Mood, List<String>> sunny = new EnumMap<>(Mood.class);
        sunny.put(Mood.HAPPY, List.of(
                "Go to the beach",
                "Have a picnic in the park",
                "Try outdoor photography",
                "Visit an outdoor cafe",
                "Go on a date"));
        sunny.put(Mood.SAD, List.of(
                "Go out with your friends",
                "Take a walk in nature",
                "Visit a botanical garden",
                "Try outdoor yoga",
                "Read a book in the sunshine"));
        sunny.put(Mood.ENERGETIC, List.of(
                "Go for a run",
                "Play beach volleyball",
                "Try rock climbing",
                "Go cycling"));
        sunny.put(Mood.RELAXED, List.of(
                "Read a book outside",
                "Have a coffee in a garden",
                "Go fishing",
                "Try meditation in the park"));

        Map<Mood, List<String>> rainy = new EnumMap<>(Mood.class);
        rainy.put(Mood.HAPPY, List.of(
                "Bake cookies",
                "Watch your favorite movie",
                "Visit a museum",
                "Try a new board game"));
        rainy.put(Mood.SAD, List.of(
                "Listen to calming music",
                "Write in a journal",
                "Try indoor gardening",
                "Make some art"));
        rainy.put(Mood.ENERGETIC, List.of(
                "Go to an indoor pool",
                "Try indoor rock climbing",
                "Dance to upbeat music",
                "Do a home workout"));
        rainy.put(Mood.RELAXED, List.of(
                "Make hot chocolate",
                "Read by the window",
                "Do a puzzle",
                "Watch some instagram reels",
                "Try knitting or crafting"));

        Map<Mood, List<String>> cloudy = new EnumMap<>(Mood.class);
        cloudy.put(Mood.HAPPY, List.of(
                "Visit a coffee shop",
                "Go to a bookstore",
                "Try a new restaurant",
                "Visit an art gallery"));
        cloudy.put(Mood.SAD, List.of(
                "Watch a comedy show",
                "Call a friend",
                "Bake something sweet",
                "Write positive affirmations"));
        cloudy.put(Mood.ENERGETIC, List.of(
                "Go for a brisk walk",
                "Try an indoor sport",
                "Clean and reorganize",
                "Learn a new dance"));
        cloudy.put(Mood.RELAXED, List.of(
                "Listen to a podcast",
                "Try gentle yoga",
                "Do some light reading",
                "Take a long bath"));

        Map<Mood, List<String>> snowy = new EnumMap<>(Mood.class);
        snowy.put(Mood.HAPPY, List.of(
                "Build a snowman",
                "Go sledding",
                "Have a snowball fight",
                "Make snow angels"));
        snowy.put(Mood.SAD, List.of(
                "Drink hot cocoa by the fire",
                "Watch snow fall from indoors",
                "Write a letter to a friend",
                "Look at old photos"));
        snowy.put(Mood.ENERGETIC, List.of(
                "Go skiing or snowboarding",
                "Try ice skating",
                "Shovel snow for exercise",
                "Do winter hiking"));
        snowy.put(Mood.RELAXED, List.of(
                "Read by the fireplace",
                "Do winter crafts",
                "Listen to calming music",
                "Make a winter stew"));

        Map<Mood, List<String>> stormy = new EnumMap<>(Mood.class);
        stormy.put(Mood.HAPPY, List.of(
                "Have a movie marathon",
                "Play some co-op games with friends",
                "Cook something new",
                "Watch the lightning from a safe window"));
        stormy.put(Mood.SAD, List.of(
                "Wrap up in a blanket with tea",
                "Call someone you miss",
                "Turn the lights down and listen to the rain",
                "Write down what's on your mind"));
        stormy.put(Mood.ENERGETIC, List.of(
                "Do a home workout",
                "Deep clean your room",
                "Learn a song on an instrument",
                "Start that side project"));
        stormy.put(Mood.RELAXED, List.of(
                "Take a nap to the sound of thunder",
                "Read a thriller",
                "Make some ginger tea",
                "Charge your devices and chill"));

        Map<Mood, List<String>> foggy = new EnumMap<>(Mood.class);
        foggy.put(Mood.HAPPY, List.of(
                "Take moody photos of the fog",
                "Go for a slow morning walk",
                "Grab breakfast at a cafe",
                "Visit a library"));
        foggy.put(Mood.SAD, List.of(
                "Make a warm bowl of soup",
                "Listen to a comfort album",
                "Text an old friend",
                "Watch a feel-good movie"));
        foggy.put(Mood.ENERGETIC, List.of(
                "Go to the gym",
                "Try a new recipe",
                "Rearrange your room",
                "Do a quick indoor workout"));
        foggy.put(Mood.RELAXED, List.of(
                "Read with a cup of coffee",
                "Try some light stretching",
                "Listen to lo-fi music",
                "Sleep in a little"));

        RECOMMENDATIONS.put(Sky.CLEAR, sunny);
        RECOMMENDATIONS.put(Sky.CLOUDY, cloudy);
        RECOMMENDATIONS.put(Sky.LIGHT_RAIN, rainy);
        RECOMMENDATIONS.put(Sky.HEAVY_RAIN, rainy);
        RECOMMENDATIONS.put(Sky.SNOW, snowy);
        RECOMMENDATIONS.put(Sky.THUNDER, stormy);
        RECOMMENDATIONS.put(Sky.MIST, foggy);
    }

    private MoodRecommendationEngine() {
    }

    public static List<String> getRecommendations(Sky sky, Mood mood) {
        if (sky == null || mood == null) {
            return List.of("Search for a city first to get some ideas");
        }
        Map<Mood, List<String>> byMood = RECOMMENDATIONS.get(sky);
        if (byMood == null) {
            return List.of("Enjoy your day!", "Try something new today", "Make the most of the weather");
        }
        return byMood.getOrDefault(mood, byMood.get(Mood.HAPPY));
    }
}
