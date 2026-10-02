package com.weatherfx.comments;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

// one JSON object per line, so a comment with newlines or "|" in it can't break the file
public class CommentStore {

    private final Path file;
    private final Gson gson = new Gson();

    public CommentStore(Path file) {
        this.file = file;
    }

    public synchronized List<Comment> loadAll() throws IOException {
        List<Comment> comments = new ArrayList<>();
        if (!Files.exists(file)) {
            return comments;
        }
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line.isBlank()) {
                continue;
            }
            try {
                comments.add(gson.fromJson(line, Comment.class));
            } catch (JsonParseException e) {
                System.err.println("Skipping a broken line in " + file + ": " + line);
            }
        }
        return comments;
    }

    public synchronized void add(Comment comment) throws IOException {
        Files.writeString(file, gson.toJson(comment) + System.lineSeparator(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}
