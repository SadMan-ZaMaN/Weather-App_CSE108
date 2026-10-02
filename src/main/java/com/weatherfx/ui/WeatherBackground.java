package com.weatherfx.ui;

import com.weatherfx.model.Sky;
import javafx.animation.AnimationTimer;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The animated sky behind every screen.
 * <p>
 * The first version of the app looped video clips here. Big video files don't play nicely with git,
 * so now everything is drawn on a canvas instead: a gradient sky plus clouds, rain, snow, fog,
 * stars or lightning depending on the weather. If a matching clip (e.g. {@code Heavy_Rain.mp4}) is
 * put in {@code resources/com/weatherfx/videos/}, that gets played instead.
 */
public class WeatherBackground extends Pane {

    private static final String VIDEO_FOLDER = "/com/weatherfx/videos/";
    private static final double FADE_SECONDS = 1.2;
    private static final double WIND = -0.18; // how much the rain leans
    private static final double CLOUD_RATIO = 180.0 / 340.0;

    private final Canvas canvas = new Canvas();
    private final MediaView videoView = new MediaView();
    private final Region shade = new Region();
    private final Random random = new Random();

    private final Image lightCloud = cloudSprite(Color.WHITE);
    private final Image darkCloud = cloudSprite(Color.web("#6b7684"));

    private final List<Cloud> clouds = new ArrayList<>();
    private final List<Drop> drops = new ArrayList<>();
    private final List<Flake> flakes = new ArrayList<>();
    private final List<Star> stars = new ArrayList<>();

    private Sky sky; // null until the first weather report comes in
    private boolean day = true;
    private Palette from = Palette.IDLE;
    private Palette to = Palette.IDLE;
    private double fade = 1; // goes 0 -> 1 while blending into a new sky
    private double time;
    private boolean needsRebuild = true;

    private double flash;
    private int flickers;
    private double nextStrike = 3;
    private double[] boltX;
    private double[] boltY;

    private MediaPlayer player;
    private long lastFrame;

    private final AnimationTimer timer = new AnimationTimer() {
        @Override
        public void handle(long now) {
            double dt = lastFrame == 0 ? 0 : Math.min((now - lastFrame) / 1e9, 0.05);
            lastFrame = now;
            update(dt);
            draw();
        }
    };

    public WeatherBackground() {
        shade.getStyleClass().add("background-shade");
        videoView.setVisible(false);
        getChildren().addAll(canvas, videoView, shade);
        setMouseTransparent(true);

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        setClip(clip);

        timer.start();
    }

    public void show(Sky sky, boolean day) {
        if (sky == this.sky && day == this.day) {
            return;
        }
        this.sky = sky;
        this.day = day;
        if (!playVideo(sky.videoName(day))) {
            useCanvas();
        }
    }

    public void dispose() {
        timer.stop();
        stopVideo();
    }

    // ---------------------------------------------------------------- layout

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        canvas.setWidth(w);
        canvas.setHeight(h);
        shade.resizeRelocate(0, 0, w, h);

        if (player != null) {
            Media media = player.getMedia();
            double vw = media.getWidth();
            double vh = media.getHeight();
            if (vw > 0 && vh > 0) {
                // scale like CSS "cover": fill the window and crop whatever sticks out
                double scale = Math.max(w / vw, h / vh);
                videoView.setFitWidth(vw * scale);
                videoView.setFitHeight(vh * scale);
                videoView.relocate((w - vw * scale) / 2, (h - vh * scale) / 2);
            }
        }
    }

    // the background should never push the window size around
    @Override
    protected double computePrefWidth(double height) {
        return 0;
    }

    @Override
    protected double computePrefHeight(double width) {
        return 0;
    }

    // ---------------------------------------------------------------- video

    private boolean playVideo(String name) {
        URL url = WeatherBackground.class.getResource(VIDEO_FOLDER + name + ".mp4");
        if (url == null) {
            return false;
        }
        try {
            MediaPlayer next = new MediaPlayer(new Media(url.toExternalForm()));
            next.setMute(true);
            next.setCycleCount(MediaPlayer.INDEFINITE);
            next.setOnReady(this::requestLayout);
            next.setOnPlaying(() -> {
                if (player == next) {
                    timer.stop();
                    canvas.setVisible(false);
                }
            });
            next.setOnError(() -> {
                System.err.println("Couldn't play " + name + ".mp4: " + next.getError());
                if (player == next) {
                    useCanvas();
                }
            });

            stopVideo();
            player = next;
            videoView.setMediaPlayer(next);
            videoView.setVisible(true);
            next.play();
            return true;
        } catch (Exception e) {
            System.err.println("Couldn't play " + name + ".mp4: " + e.getMessage());
            return false;
        }
    }

    private void stopVideo() {
        if (player != null) {
            player.dispose();
            player = null;
        }
        videoView.setMediaPlayer(null);
        videoView.setVisible(false);
    }

    private void useCanvas() {
        stopVideo();
        canvas.setVisible(true);
        from = currentPalette();
        to = Palette.of(sky, day);
        fade = 0;
        needsRebuild = true;
        lastFrame = 0;
        timer.start();
    }

    // ---------------------------------------------------------------- animation

    private void rebuild(double w, double h) {
        clouds.clear();
        drops.clear();
        flakes.clear();
        stars.clear();

        int cloudCount = 0;
        int dropCount = 0;
        int flakeCount = 0;
        int starCount = 0;
        int fogCount = 0;

        if (sky == null) {
            cloudCount = 4;
            starCount = 40;
        } else {
            switch (sky) {
                case CLEAR -> {
                    cloudCount = day ? 2 : 0;
                    starCount = day ? 0 : 160;
                }
                case CLOUDY -> {
                    cloudCount = 9;
                    starCount = day ? 0 : 25;
                }
                case LIGHT_RAIN -> {
                    cloudCount = 7;
                    dropCount = 140;
                }
                case HEAVY_RAIN -> {
                    cloudCount = 9;
                    dropCount = 420;
                }
                case SNOW -> {
                    cloudCount = 5;
                    flakeCount = 220;
                }
                case MIST -> fogCount = 11;
                case THUNDER -> {
                    cloudCount = 10;
                    dropCount = 260;
                }
            }
        }

        boolean gloomy = sky == Sky.LIGHT_RAIN || sky == Sky.HEAVY_RAIN || sky == Sky.THUNDER || (sky != null && !day);
        for (int i = 0; i < cloudCount; i++) {
            Cloud c = new Cloud();
            c.width = w * (0.25 + random.nextDouble() * 0.3);
            c.x = -c.width + random.nextDouble() * (w + c.width);
            c.y = h * (-0.08 + random.nextDouble() * 0.45);
            c.speed = 6 + random.nextDouble() * 14;
            c.alpha = sky == null ? 0.18 : 0.45 + random.nextDouble() * 0.45;
            c.dark = gloomy;
            clouds.add(c);
        }
        for (int i = 0; i < fogCount; i++) {
            Cloud c = new Cloud();
            c.width = w * (1.0 + random.nextDouble() * 0.8);
            c.x = -c.width + random.nextDouble() * (w + c.width);
            c.y = h * (0.15 + random.nextDouble() * 0.8);
            c.speed = 8 + random.nextDouble() * 14;
            c.alpha = day ? 0.35 : 0.18;
            c.fog = true;
            clouds.add(c);
        }
        for (int i = 0; i < dropCount; i++) {
            Drop d = new Drop();
            boolean heavy = sky != Sky.LIGHT_RAIN;
            d.length = heavy ? 16 + random.nextDouble() * 18 : 10 + random.nextDouble() * 12;
            d.speed = heavy ? 900 + random.nextDouble() * 500 : 600 + random.nextDouble() * 350;
            resetDrop(d, w, h, true);
            drops.add(d);
        }
        for (int i = 0; i < flakeCount; i++) {
            Flake f = new Flake();
            f.x = random.nextDouble() * w;
            f.y = random.nextDouble() * h;
            f.radius = 1.2 + random.nextDouble() * 2.8;
            f.speed = 25 + f.radius * 15 + random.nextDouble() * 20; // bigger flakes are closer, so faster
            f.wobble = 0.5 + random.nextDouble() * 1.5;
            f.phase = random.nextDouble() * Math.PI * 2;
            flakes.add(f);
        }
        for (int i = 0; i < starCount; i++) {
            Star s = new Star();
            s.x = random.nextDouble();
            s.y = random.nextDouble() * 0.7;
            s.radius = 0.5 + random.nextDouble() * 1.2;
            s.speed = 0.5 + random.nextDouble() * 2;
            s.phase = random.nextDouble() * Math.PI * 2;
            stars.add(s);
        }
    }

    private void resetDrop(Drop d, double w, double h, boolean anywhere) {
        // spawn a bit to the right of the screen too, since the wind pushes drops left
        d.x = random.nextDouble() * w * 1.25;
        d.y = anywhere ? random.nextDouble() * h : -d.length - random.nextDouble() * 80;
    }

    private void update(double dt) {
        time += dt;
        fade = Math.min(1, fade + dt / FADE_SECONDS);

        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w < 1 || h < 1) {
            return;
        }
        if (needsRebuild) {
            rebuild(w, h);
            needsRebuild = false;
        }

        for (Cloud c : clouds) {
            c.x += c.speed * dt;
            if (c.x > w) {
                c.x = -c.width;
            }
        }
        for (Drop d : drops) {
            d.y += d.speed * dt;
            d.x += d.speed * WIND * dt;
            if (d.y > h) {
                resetDrop(d, w, h, false);
            }
        }
        for (Flake f : flakes) {
            f.y += f.speed * dt;
            f.x += Math.sin(time * f.wobble + f.phase) * 20 * dt;
            if (f.y > h + 5) {
                f.y = -5;
                f.x = random.nextDouble() * w;
            }
        }

        if (sky == Sky.THUNDER) {
            updateLightning(dt, w, h);
        } else {
            flash = 0;
        }
    }

    private void updateLightning(double dt, double w, double h) {
        nextStrike -= dt;
        if (nextStrike <= 0) {
            flash = 1;
            flickers = 1 + random.nextInt(2);
            makeBolt(w, h);
            nextStrike = 4 + random.nextDouble() * 7;
        }
        flash = Math.max(0, flash - dt * 3);
        if (flash > 0 && flash < 0.35 && flickers > 0) {
            flash = 0.8;
            flickers--;
        }
    }

    private void makeBolt(double w, double h) {
        int points = 9 + random.nextInt(5);
        boltX = new double[points];
        boltY = new double[points];
        double x = w * (0.15 + random.nextDouble() * 0.7);
        double y = 0;
        double step = h * 0.6 / (points - 1);
        for (int i = 0; i < points; i++) {
            boltX[i] = x;
            boltY[i] = y;
            x += (random.nextDouble() - 0.5) * 60;
            y += step * (0.7 + random.nextDouble() * 0.6);
        }
    }

    // ---------------------------------------------------------------- drawing

    private void draw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        Palette palette = currentPalette();

        g.setGlobalAlpha(1);
        g.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, palette.top()), new Stop(1, palette.bottom())));
        g.fillRect(0, 0, w, h);

        // everything on top of the sky fades in together with the new colours
        double alpha = fade;

        drawStars(g, w, h, alpha);
        if (sky == Sky.CLEAR && day) {
            drawSun(g, w, h, alpha);
        } else if (sky == Sky.CLEAR) {
            drawMoon(g, w, h, alpha);
        }
        drawClouds(g, alpha);
        drawRain(g, alpha);
        drawSnow(g, alpha);
        drawLightning(g, w, h);

        g.setGlobalAlpha(1);
    }

    private void drawStars(GraphicsContext g, double w, double h, double alpha) {
        g.setFill(Color.WHITE);
        for (Star s : stars) {
            double twinkle = 0.55 + 0.45 * Math.sin(time * s.speed + s.phase);
            g.setGlobalAlpha(alpha * twinkle);
            g.fillOval(s.x * w, s.y * h, s.radius * 2, s.radius * 2);
        }
    }

    private void drawSun(GraphicsContext g, double w, double h, double alpha) {
        double cx = w * 0.82;
        double cy = h * 0.14;
        double r = Math.min(w, h) * (0.55 + 0.02 * Math.sin(time * 0.7));
        g.setGlobalAlpha(alpha);
        g.setFill(new RadialGradient(0, 0, cx, cy, r, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(255, 250, 225, 0.95)),
                new Stop(0.1, Color.rgb(255, 230, 160, 0.55)),
                new Stop(1, Color.rgb(255, 210, 120, 0))));
        g.fillRect(0, 0, w, h);
    }

    private void drawMoon(GraphicsContext g, double w, double h, double alpha) {
        double cx = w * 0.8;
        double cy = h * 0.17;
        double r = 26;
        g.setGlobalAlpha(alpha);
        g.setFill(new RadialGradient(0, 0, cx, cy, r * 6, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(220, 230, 255, 0.28)),
                new Stop(1, Color.rgb(220, 230, 255, 0))));
        g.fillOval(cx - r * 6, cy - r * 6, r * 12, r * 12);
        g.setFill(Color.web("#f4f1e4"));
        g.fillOval(cx - r, cy - r, r * 2, r * 2);
    }

    private void drawClouds(GraphicsContext g, double alpha) {
        for (Cloud c : clouds) {
            g.setGlobalAlpha(alpha * c.alpha);
            Image sprite = c.dark ? darkCloud : lightCloud;
            double height = c.fog ? c.width * 0.22 : c.width * CLOUD_RATIO;
            g.drawImage(sprite, c.x, c.y, c.width, height);
        }
    }

    private void drawRain(GraphicsContext g, double alpha) {
        if (drops.isEmpty()) {
            return;
        }
        g.setGlobalAlpha(alpha);
        g.setStroke(Color.rgb(200, 215, 240, sky == Sky.LIGHT_RAIN ? 0.45 : 0.6));
        g.setLineWidth(sky == Sky.HEAVY_RAIN ? 1.4 : 1.1);
        for (Drop d : drops) {
            g.strokeLine(d.x, d.y, d.x + d.length * WIND, d.y + d.length);
        }
    }

    private void drawSnow(GraphicsContext g, double alpha) {
        if (flakes.isEmpty()) {
            return;
        }
        g.setGlobalAlpha(alpha);
        g.setFill(Color.rgb(255, 255, 255, 0.85));
        for (Flake f : flakes) {
            g.fillOval(f.x - f.radius, f.y - f.radius, f.radius * 2, f.radius * 2);
        }
    }

    private void drawLightning(GraphicsContext g, double w, double h) {
        if (flash <= 0) {
            return;
        }
        g.setGlobalAlpha(1);
        g.setFill(Color.rgb(225, 232, 255, flash * 0.35));
        g.fillRect(0, 0, w, h);
        if (boltX != null && flash > 0.4) {
            g.setStroke(Color.rgb(255, 255, 255, flash));
            g.setLineWidth(2.2);
            g.strokePolyline(boltX, boltY, boltX.length);
        }
    }

    private Palette currentPalette() {
        return from.blend(to, fade);
    }

    /** A soft cloud made of a few overlapping blurry circles, rendered once and reused every frame. */
    private static Image cloudSprite(Color color) {
        Canvas sprite = new Canvas(340, 180);
        GraphicsContext g = sprite.getGraphicsContext2D();
        double[][] puffs = {{80, 105, 60}, {145, 80, 72}, {215, 88, 62}, {268, 110, 48}, {175, 118, 58}};
        Color solid = color.deriveColor(0, 1, 1, 0.55);
        Color clear = color.deriveColor(0, 1, 1, 0);
        for (double[] p : puffs) {
            g.setFill(new RadialGradient(0, 0, p[0], p[1], p[2], false, CycleMethod.NO_CYCLE,
                    new Stop(0, solid), new Stop(1, clear)));
            g.fillOval(p[0] - p[2], p[1] - p[2], p[2] * 2, p[2] * 2);
        }
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        return sprite.snapshot(params, null);
    }

    private record Palette(Color top, Color bottom) {

        static final Palette IDLE = new Palette(Color.web("#14213d"), Color.web("#3a5a8c"));

        static Palette of(Sky sky, boolean day) {
            if (sky == null) {
                return IDLE;
            }
            return switch (sky) {
                case CLEAR -> day ? of("#2b6fd6", "#7cc4f2") : of("#070b1f", "#24325a");
                case CLOUDY -> day ? of("#4f6d8c", "#a3b6c9") : of("#151d2b", "#384659");
                case LIGHT_RAIN -> day ? of("#3d5166", "#7d90a3") : of("#10161f", "#2b3847");
                case HEAVY_RAIN -> day ? of("#2c3a4a", "#5d6f80") : of("#0b1017", "#222d39");
                case SNOW -> day ? of("#6f88a3", "#c3d2e1") : of("#18263a", "#475b73");
                case MIST -> day ? of("#7c8a96", "#bcc6ce") : of("#22292f", "#4a545c");
                case THUNDER -> day ? of("#232a3b", "#4a5367") : of("#0c0f18", "#262c3b");
            };
        }

        private static Palette of(String top, String bottom) {
            return new Palette(Color.web(top), Color.web(bottom));
        }

        Palette blend(Palette other, double t) {
            return new Palette(top.interpolate(other.top, t), bottom.interpolate(other.bottom, t));
        }
    }

    private static final class Cloud {
        double x, y, width, speed, alpha;
        boolean dark, fog;
    }

    private static final class Drop {
        double x, y, length, speed;
    }

    private static final class Flake {
        double x, y, radius, speed, wobble, phase;
    }

    private static final class Star {
        double x, y, radius, speed, phase;
    }
}
