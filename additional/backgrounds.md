# How the backgrounds work

The sky behind the app changes with the weather: rain when it's raining, snow when it's snowing, stars at night. This page explains how that works, how to change it and how to add a new kind of weather.

## Contents

1. [Short answer: images, videos or code?](#1-short-answer-images-videos-or-code)
2. [What happens when you search for a city](#2-what-happens-when-you-search-for-a-city)
3. [Files involved](#3-files-involved)
4. [From weather text to a sky type](#4-from-weather-text-to-a-sky-type)
5. [The animation engine](#5-the-animation-engine)
6. [The effects one by one](#6-the-effects-one-by-one)
7. [Sky colours](#7-sky-colours)
8. [Optional video backgrounds](#8-optional-video-backgrounds)
9. [Changing the look](#9-changing-the-look)
10. [Adding a new weather condition](#10-adding-a-new-weather-condition)
11. [Previewing a sky without waiting for the weather](#11-previewing-a-sky-without-waiting-for-the-weather)
12. [Performance](#12-performance)
13. [Glossary](#13-glossary)

---

## 1. Short answer: images, videos or code?

**Code.** The project has no background images or video files. The sky, clouds, rain, snow, fog, stars, sun, moon and lightning are all drawn by Java code, about 60 times a second, on a JavaFX `Canvas`.

- The soft clouds aren't image files either. They're drawn once in memory when the app starts and then reused (see [Clouds](#clouds)).
- The only image files the app uses are the small icons on the weather cards, in `src/main/resources/com/weatherfx/icons/`.
- The pictures in `docs/screenshots/` are screenshots for the README. The app doesn't use them.

The first version of the app played video clips here. They were swapped for drawn backgrounds because large video files don't belong in a git repository. Video support is still built in, though: if you put `.mp4` clips in the videos folder, the app plays them instead (see [section 8](#8-optional-video-backgrounds)).

---

## 2. What happens when you search for a city

```
WeatherAPI answers:  condition = "Patchy light rain with thunder", is_day = 1
                                   │
                                   ▼
Sky.fromCondition(...)      →   Sky.THUNDER
                                   │
                                   ▼
WeatherBackground.show(THUNDER, day = true)
                                   │
               Is there a Thunder.mp4 in the videos folder?
                     │ yes                         │ no  (the default)
                     ▼                             ▼
          play the clip, muted           draw the sky on a Canvas,
          and looping                    about 60 times a second
```

The background sits behind every screen (main, forecast, comments), so all of them share the same sky. It always shows the **current** weather of the city you last searched. Picking a different day on the forecast screen doesn't change it.

---

## 3. Files involved

| File | What it does |
|---|---|
| [`model/Sky.java`](../src/main/java/com/weatherfx/model/Sky.java) | The 7 sky types, the mapping from weather text to a type, and the video file names |
| [`ui/WeatherBackground.java`](../src/main/java/com/weatherfx/ui/WeatherBackground.java) | Everything that gets drawn or played. Almost all of this page is about this file |
| [`ui/MainController.java`](../src/main/java/com/weatherfx/ui/MainController.java) | Calls `background().show(...)` after every successful search |
| [`WeatherApp.java`](../src/main/java/com/weatherfx/WeatherApp.java) | Creates the background and stacks the screens on top of it |
| [`api/WeatherParser.java`](../src/main/java/com/weatherfx/api/WeatherParser.java) | Reads the condition text and the `is_day` flag from the API response |
| [`css/app.css`](../src/main/resources/com/weatherfx/css/app.css) | `.background-shade`: a dark gradient over the sky that keeps text readable |
| [`resources/.../videos/`](../src/main/resources/com/weatherfx/videos/) | Optional video clips (empty by default) |
| [`model/SkyTest.java`](../src/test/java/com/weatherfx/model/SkyTest.java) | Tests that the weather texts map to the right sky type |

The Java files are in `src/main/java/com/weatherfx/`. Click a name to open the file.

---

## 4. From weather text to a sky type

WeatherAPI describes the weather with about 50 different texts, such as "Light drizzle", "Moderate or heavy sleet" or "Thundery outbreaks in nearby". Drawing 50 different skies would be overkill, so `Sky.fromCondition()` reduces them to **7 types**:

`CLEAR`, `CLOUDY`, `LIGHT_RAIN`, `HEAVY_RAIN`, `SNOW`, `MIST`, `THUNDER`

It converts the text to lowercase and checks for keywords **in this order**. The first match wins:

| Checked | If the text contains | Sky type | Example texts |
|:-:|---|---|---|
| 1 | `thunder` | `THUNDER` | Thundery outbreaks in nearby, Patchy light rain with thunder |
| 2 | `snow`, `sleet`, `ice`, `blizzard` | `SNOW` | Light snow, Blizzard, Ice pellets, Moderate or heavy sleet |
| 3 | `heavy`, `torrential`, `moderate rain` | `HEAVY_RAIN` | Moderate rain at times, Heavy rain, Torrential rain shower |
| 4 | `rain`, `drizzle`, `shower` | `LIGHT_RAIN` | Patchy rain nearby, Light drizzle, Light rain shower |
| 5 | `mist`, `fog`, `haze` | `MIST` | Mist, Fog, Freezing fog |
| 6 | `cloud`, `overcast` | `CLOUDY` | Partly cloudy, Cloudy, Overcast |
| – | anything else | `CLEAR` | Sunny, Clear, or a text the code doesn't recognise |

**Why the order matters:**
- "Moderate or heavy snow with thunder" contains both "snow" and "thunder". Thunder is checked first, so it's a storm.
- "Heavy snow" contains "heavy", but snow is checked before "heavy", so it's snow, not heavy rain.

**Day or night** comes from the API's `is_day` field. It reflects daylight **at the city you searched**, not your computer's clock. Searching for a city on the other side of the world at your lunchtime will show a night sky.

The same 7 types also drive the mood tips in `mood/MoodRecommendationEngine.java`.

---

## 5. The animation engine

### The loop

`WeatherBackground` runs a JavaFX `AnimationTimer`. JavaFX calls its `handle()` method once for every frame the screen draws, usually 60 times a second. Each call does two things:

1. **`update(dt)`** moves everything forward in time. `dt` is the number of seconds since the last frame, so all speeds in the code are **pixels per second**. This keeps the animation the same speed on a 60 Hz and a 144 Hz monitor. `dt` is capped at 0.05 s, so after a pause (for example while you drag the window) nothing jumps across the screen.
2. **`draw()`** clears the canvas and repaints the whole scene from scratch.

### Layers

`draw()` paints from back to front:

1. Sky gradient
2. Stars
3. Sun (clear day) or moon (clear night)
4. Clouds and fog
5. Rain
6. Snow
7. Lightning flash and bolt

On top of the canvas sits the `.background-shade` overlay from `app.css`. It's a separate node that darkens the top and bottom edges so white text stays readable on bright skies. Above that are the app's screens.

### Switching weather

When `show()` is called with a new sky type:
- the sky colours **blend** from the current colours to the new ones over 1.2 seconds (`FADE_SECONDS`);
- the old clouds, drops and flakes are removed, and the new ones **fade in** over the same 1.2 seconds.

If `show()` is called with the same sky type and the same day/night as before, it does nothing. Searching the same city again doesn't restart the animation.

### Recycling

No new objects are created while the animation runs. A raindrop that falls past the bottom is moved back above the top at a new random spot. A cloud that drifts off the right edge comes back in from the left. Snowflakes start again at the top. The particles are created once, in `rebuild()`, whenever the weather changes.

---

## 6. The effects one by one

### What each sky type contains

| Sky type | Clouds | Raindrops | Snowflakes | Stars | Fog banks | Extras |
|---|:-:|:-:|:-:|:-:|:-:|---|
| Before the first search | 4 (faint) | – | – | 40 | – | – |
| `CLEAR` | 2 by day, 0 at night | – | – | 160 at night | – | Sun by day, moon at night |
| `CLOUDY` | 9 | – | – | 25 at night | – | – |
| `LIGHT_RAIN` | 7 | 140 | – | – | – | – |
| `HEAVY_RAIN` | 9 | 420 | – | – | – | – |
| `SNOW` | 5 | – | 220 | – | – | – |
| `MIST` | – | – | – | – | 11 | – |
| `THUNDER` | 10 | 260 | – | – | – | Lightning |

These numbers are set in `rebuild()`.

### Sky

A vertical gradient between two colours, a "top" and a "bottom". Every sky type has its own pair for day and for night (see [section 7](#7-sky-colours)).

### Clouds

Each cloud is drawn from one **sprite**, a small picture that the code draws once at startup in `cloudSprite()`:

- 5 overlapping circles, each filled with a radial gradient that goes from 55% opaque in the middle to fully transparent at the edge. Together they make one soft, puffy cloud.
- The picture is saved with `snapshot()` and reused every frame, which is much cheaper than redrawing 5 gradients per cloud.
- There are two copies: **white** for normal skies and **dark grey** (`#6b7684`) for rain, thunder and night.

Every cloud on screen is that sprite at a random size (25–55% of the window width), height (from just above the top down to about 37% of the window), opacity (45–90%) and speed (6–20 px/s, drifting left to right).

### Rain

Each raindrop is **one thin line**. It looks good because:

- **Each drop has its own speed and length.** Light rain falls at 600–950 px/s with 10–22 px streaks. Heavy rain and thunderstorms fall at 900–1400 px/s with 16–34 px streaks. Mixing speeds gives a sense of depth: fast drops seem closer.
- **The slant matches the motion.** `WIND = -0.18` means a drop moves 0.18 px left for every pixel it falls, about 10°. The same number tilts both the movement and the line itself, so every streak points exactly the way it's travelling.
- **The colour is soft.** Pale blue-white (`rgb(200, 215, 240)`) at 45% opacity for light rain and 60% for heavy rain, rather than solid white.
- **No empty edge.** The wind pushes drops left, so new drops start anywhere from the left edge to a quarter of a window width past the right edge. Without that, the right side of the screen would slowly empty out.

### Snow

Each flake is a small white dot (radius 1.2–4 px, 85% opacity).
- **Bigger flakes fall faster** (about 43–105 px/s), so they read as closer to you.
- Each flake **sways** sideways along a sine wave with its own speed and starting point, so they don't all move together.

### Fog (mist)

Fog reuses the white cloud sprite, stretched very wide (100–180% of the window) and flattened to a thin band. The 11 bands are spread from about 15% of the way down the window to the bottom, and drift slowly sideways. They're more see-through at night (18%) than by day (35%).

### Stars

Small white dots in the top 70% of the window. Each one **twinkles**: its brightness follows its own sine wave between 10% and 100%. Positions are stored as fractions of the window size, so stars stay in place when you resize the window.

### Sun

Not a disc, but a large soft glow in the top-right corner: a radial gradient from warm white to transparent, with a radius of about 55% of the window's smaller side. Its size slowly grows and shrinks a little, so it seems to breathe.

### Moon

A cream-coloured disc (`#f4f1e4`, radius 26 px) with a faint bluish halo six times its size. It only appears on clear nights.

### Lightning

Every **4–11 seconds** (random each time) during a thunderstorm:

1. The whole screen gets a pale blue-white **flash** (up to 35% opacity) that fades out in about a third of a second.
2. As it fades, it **flickers** back to bright one or two extra times, like real lightning.
3. While the flash is bright, a **bolt** is drawn: a jagged line of 9–13 points from the top of the window down to about 60% of its height. Each point is shifted up to 30 px left or right at random, so every bolt looks different.

---

## 7. Sky colours

From `Palette.of()` in `WeatherBackground.java`. Each entry is *top colour → bottom colour*.

| Sky type | Day | Night |
|---|---|---|
| Before the first search | `#14213d` → `#3a5a8c` | (same) |
| `CLEAR` | `#2b6fd6` → `#7cc4f2` | `#070b1f` → `#24325a` |
| `CLOUDY` | `#4f6d8c` → `#a3b6c9` | `#151d2b` → `#384659` |
| `LIGHT_RAIN` | `#3d5166` → `#7d90a3` | `#10161f` → `#2b3847` |
| `HEAVY_RAIN` | `#2c3a4a` → `#5d6f80` | `#0b1017` → `#222d39` |
| `SNOW` | `#6f88a3` → `#c3d2e1` | `#18263a` → `#475b73` |
| `MIST` | `#7c8a96` → `#bcc6ce` | `#22292f` → `#4a545c` |
| `THUNDER` | `#232a3b` → `#4a5367` | `#0c0f18` → `#262c3b` |

---

## 8. Optional video backgrounds

### How it works

Before drawing anything, `show()` looks for a clip in `src/main/resources/com/weatherfx/videos/` named after the sky type:

| Weather | File name |
|---|---|
| Clear, day | `Sunny.mp4` |
| Clear, night | `Clear.mp4` |
| Cloudy / overcast | `Cloudy.mp4` |
| Drizzle, light showers | `Light_Rain.mp4` |
| Moderate / heavy rain | `Heavy_Rain.mp4` |
| Snow, sleet, ice | `Snow.mp4` |
| Mist / fog | `Mist.mp4` |
| Thunderstorms | `Thunder.mp4` |

These names come from `Sky.videoName()`.

- **Found:** the clip plays **muted** and **on a loop**, scaled to fill the window and cropped at the edges (like CSS `background-size: cover`). The drawing loop stops while a video plays, to save CPU.
- **Not found, or can't play:** the app draws the sky instead. If a clip fails to load, the error is printed to the console and the drawn sky takes over.

You can mix the two: add only `Thunder.mp4`, and storms use the video while everything else stays drawn.

The clips live inside `resources`, so they're packaged with the app when it's built. After adding one, run the app again.

### Making a clip that works well

JavaFX plays **MP4 files with H.264 video**. A good target is 1280 or 1920 px wide, 10–20 seconds long, and a few MB in size. Clips that loop seamlessly (where the last frame flows into the first) look best, because the clip repeats forever.

[ffmpeg](https://ffmpeg.org/) can convert almost any clip into the right format:

```bash
ffmpeg -i input.mov -t 12 -vf "scale=1280:-2" -c:v libx264 -pix_fmt yuv420p -crf 26 -preset slow -an -movflags +faststart Heavy_Rain.mp4
```

| Part | Meaning |
|---|---|
| `-t 12` | keep only the first 12 seconds |
| `scale=1280:-2` | resize to 1280 px wide and keep the shape |
| `-c:v libx264 -pix_fmt yuv420p` | H.264 in a pixel format JavaFX can play |
| `-crf 26` | quality: higher numbers mean a smaller file and lower quality (about 20–30 is sensible) |
| `-an` | remove the audio (the app mutes it anyway) |
| `-movflags +faststart` | lets playback start without reading the whole file first |

On Linux, JavaFX needs the system's libavcodec/libavformat libraries to play H.264. If they're missing, the app falls back to the drawn sky.

### Where to get clips

Record your own, or use free stock footage, for example from Pexels or Pixabay. If you push the clips to GitHub you are **redistributing** them, so check that the licence allows it.

### Videos and git

- GitHub rejects any file over **100 MB** and warns above 50 MB.
- Even small videos make the repository slower to clone for everyone.
- To keep clips on your own machine only, add this line to `.gitignore`:

  ```
  src/main/resources/com/weatherfx/videos/*.mp4
  ```

---

## 9. Changing the look

Every change below is in [`WeatherBackground.java`](../src/main/java/com/weatherfx/ui/WeatherBackground.java) unless stated otherwise. Restart the app to see the result.

| I want to change… | Where | What to edit |
|---|---|---|
| How much rain | `rebuild()` | `dropCount` in the `LIGHT_RAIN`, `HEAVY_RAIN` or `THUNDER` case |
| Rain speed or streak length | `rebuild()`, raindrop loop | the `d.length = ...` and `d.speed = ...` lines |
| Rain angle | `WIND` constant at the top | `0` = straight down, `-0.18` = current lean, `-0.4` = strong wind (see below if you make it positive) |
| Rain colour or thickness | `drawRain()` | `setStroke(Color.rgb(...))` and `setLineWidth(...)` |
| Number of clouds, snowflakes or stars | `rebuild()` | `cloudCount`, `flakeCount`, `starCount` for that sky type |
| Cloud speed | `rebuild()`, cloud loop | `c.speed = 6 + random.nextDouble() * 14` (min 6, plus up to 14 more, px/s) |
| Cloud shape | `cloudSprite()` | the `puffs` array: each entry is `{x, y, radius}` of one circle |
| Snow sway | `update()` | the `* 20` in the snowflake loop is how far they sway |
| Time between lightning strikes | `updateLightning()` | `nextStrike = 4 + random.nextDouble() * 7` (4 to 11 seconds) |
| Lightning brightness | `drawLightning()` | the `flash * 0.35` (35% maximum) |
| Sun or moon position | `drawSun()`, `drawMoon()` | `cx` and `cy` (fractions of the window width and height) |
| Sky colours | `Palette.of()` | the hex colours (see [section 7](#7-sky-colours)) |
| Speed of the change between skies | `FADE_SECONDS` | seconds; default `1.2` |
| Darkening behind the text | [`app.css`](../src/main/resources/com/weatherfx/css/app.css) | `.background-shade` gradient |

**If you make the rain lean right** (a positive `WIND`), new drops need to start to the *left* of the screen instead of the right. Otherwise the left side empties out. In `resetDrop()`, replace the `d.x = ...` line with:

```java
d.x = (WIND < 0 ? 0 : -w * 0.25) + random.nextDouble() * w * 1.25;
```

---

## 10. Adding a new weather condition

There are two different situations.

### A. A weather text shows the wrong sky

Say the API started sending a text the code doesn't recognise, such as "Smoke" (made up for this example). No keyword matches, so it falls through to `CLEAR` and you get a sunny sky. To fix it, add a keyword to the matching line in `Sky.fromCondition()`:

```java
if (text.contains("mist") || text.contains("fog") || text.contains("haze") || text.contains("smoke")) {
    return MIST;
}
```

Then add a row to the test in [`SkyTest.java`](../src/test/java/com/weatherfx/model/SkyTest.java):

```java
"Smoke, MIST",
```

and run the tests (`./mvnw test`, or `.\mvnw.cmd test` on Windows).

**Watch the order.** A keyword in an earlier `if` beats one in a later `if`. For example, adding `"freezing"` to the `SNOW` line would also turn "Freezing fog" into snow, because snow is checked before mist. The tests catch mistakes like this, which is why it's worth adding a row for every new text.

### B. A completely new look (for example hail or a sandstorm)

Here's the checklist. Some steps the compiler reminds you about, and some it doesn't.

| Step | File | Compiler reminds you? |
|---|---|---|
| 1. Add the new constant to the enum, e.g. `HAIL` | `Sky.java` | – |
| 2. Add its keywords to `fromCondition()`, in the right position | `Sky.java` | No |
| 3. Add a case to `videoName()` (the clip's file name) | `Sky.java` | **Yes**, compile error until you do |
| 4. Add a colour pair to `Palette.of()` | `WeatherBackground.java` | **Yes**, compile error until you do |
| 5. Add a case to the `switch` in `rebuild()` | `WeatherBackground.java` | **No.** Forget it and you get an empty gradient |
| 6. Add mood suggestions with `RECOMMENDATIONS.put(Sky.HAIL, ...)` | `MoodRecommendationEngine.java` | No. Without it you get a generic list |
| 7. Add test rows | `SkyTest.java` | No |
| 8. Add the clip name to the table | `videos/README.md` | No |

**Often you can reuse existing effects.** A sandstorm could be fog bands plus a brown palette, and drizzle at night could just be fewer drops. In that case step 5 is only a matter of setting the counts:

```java
case SANDSTORM -> {
    cloudCount = 4;
    fogCount = 14;
}
```

**If you need a new kind of particle**, follow the pattern raindrops and snowflakes already use:

```java
// 1. A small class for one particle (next to Drop and Flake at the bottom of the file)
private static final class Hailstone {
    double x, y, radius, speed;
}

// 2. A list to hold them (next to the other lists)
private final List<Hailstone> hail = new ArrayList<>();

// 3. In rebuild(): clear the list, add an `int hailCount = 0;` counter,
//    set it in the switch, then create the particles
hail.clear();
// ...
case HAIL -> {
    cloudCount = 9;
    hailCount = 180;
}
// ...
for (int i = 0; i < hailCount; i++) {
    Hailstone s = new Hailstone();
    s.x = random.nextDouble() * w;
    s.y = random.nextDouble() * h;
    s.radius = 1.5 + random.nextDouble() * 2;
    s.speed = 700 + random.nextDouble() * 300;
    hail.add(s);
}

// 4. In update(): move them and recycle the ones that leave the screen
for (Hailstone s : hail) {
    s.y += s.speed * dt;
    if (s.y > h) {
        s.y = -s.radius;
        s.x = random.nextDouble() * w;
    }
}

// 5. A drawing method, called from draw() at the right layer (e.g. after drawRain)
private void drawHail(GraphicsContext g, double alpha) {
    g.setGlobalAlpha(alpha);
    g.setFill(Color.rgb(235, 240, 250, 0.9));
    for (Hailstone s : hail) {
        g.fillOval(s.x - s.radius, s.y - s.radius, s.radius * 2, s.radius * 2);
    }
}
```

Multiply every movement by `dt` and pass `alpha` through to the drawing. That keeps the new effect running at the right speed on any monitor and lets it fade in with the others.

---

## 11. Previewing a sky without waiting for the weather

Waiting for a thunderstorm somewhere in the world just to see your change is slow. Instead, temporarily force a sky type. In `MainController.java`, in `load()`, find:

```java
ctx.background().show(Sky.fromCondition(now.condition()), now.day());
```

and change it to:

```java
ctx.background().show(Sky.THUNDER, false); // TEMP preview, don't commit
```

Search for any city and you'll get a night thunderstorm. Try each type with `true` (day) and `false` (night), then **put the original line back**.

---

## 12. Performance

The background is designed to run constantly without slowing the app down:

- **Simple shapes only.** At most a few hundred lines and dots per frame, and JavaFX draws canvases on the graphics card.
- **Clouds are drawn once.** The cloud picture is built a single time and then just copied onto the canvas.
- **Particles are reused.** Raindrops, flakes and clouds are moved back to the start instead of being created and thrown away every frame.
- **It stops when it isn't needed.** The drawing loop stops while a video is playing and when the app closes (`dispose()`).
- **It stays out of the way.** The background ignores the mouse, so clicks go through to the buttons, and it never changes the window's size.

---

## 13. Glossary

| Term | Meaning here |
|---|---|
| **Canvas** | A JavaFX area you can draw shapes on with code, like a blank sheet of paper |
| **AnimationTimer** | A JavaFX class whose `handle()` method runs once per screen frame (about 60 times a second) |
| **Frame** | One redraw of the screen |
| **`dt` (delta time)** | Seconds since the previous frame. Speed × `dt` = how far something moves this frame |
| **Particle** | One small moving thing: a raindrop, snowflake, star or cloud |
| **Sprite** | A small picture drawn once and reused many times (the clouds) |
| **Gradient** | A smooth blend between colours. *Linear* goes in a straight direction (the sky); *radial* spreads out from a centre point (the sun, the cloud puffs) |
| **Alpha / opacity** | How see-through something is: 0 is invisible, 1 is solid |
| **Sine wave (`Math.sin`)** | A value that smoothly goes up and down forever. Used for twinkling, swaying and the sun's breathing |
