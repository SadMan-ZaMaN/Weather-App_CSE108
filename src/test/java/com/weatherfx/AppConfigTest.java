package com.weatherfx;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void readsValuesFromFile() throws IOException {
        Path file = tempDir.resolve("config.properties");
        Files.writeString(file, "comments.host=192.168.0.10\ncomments.port=6000\n");

        AppConfig config = AppConfig.load(file);
        assertEquals("192.168.0.10", config.commentsHost());
        assertEquals(6000, config.commentsPort());
    }

    @Test
    void missingFileMeansDefaults() {
        AppConfig config = AppConfig.load(tempDir.resolve("nope.properties"));
        assertEquals("localhost", config.commentsHost());
        assertEquals(5555, config.commentsPort());
    }

    @Test
    void badPortFallsBackToDefault() throws IOException {
        Path file = tempDir.resolve("config.properties");
        Files.writeString(file, "comments.port=not-a-number\n");
        assertEquals(5555, AppConfig.load(file).commentsPort());
    }
}
