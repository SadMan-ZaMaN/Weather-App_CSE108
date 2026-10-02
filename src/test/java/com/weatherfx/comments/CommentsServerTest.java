package com.weatherfx.comments;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Spins up a real server on a free port and talks to it with the real client. */
class CommentsServerTest {

    @TempDir
    Path tempDir;

    private CommentsServer server;
    private CommentsClient client;
    private Path file;

    @BeforeEach
    void startServer() throws IOException {
        file = tempDir.resolve("comments.jsonl");
        server = new CommentsServer(0, new CommentStore(file));
        Thread thread = new Thread(server::serve, "test-comments-server");
        thread.setDaemon(true);
        thread.start();
        client = new CommentsClient("localhost", server.port());
    }

    @AfterEach
    void stopServer() throws IOException {
        server.close();
    }

    @Test
    void postedCommentsComeBackInOrder() throws IOException {
        client.post("sadman", "Hot and humid again", "Dhaka, Bangladesh");
        client.post("rafi", "Raining all day", "Chattogram, Bangladesh");

        List<Comment> comments = client.fetchAll();
        assertEquals(2, comments.size());
        assertEquals("sadman", comments.get(0).author());
        assertEquals("Raining all day", comments.get(1).text());
        assertTrue(comments.get(0).postedAt() > 0);
    }

    @Test
    void awkwardTextSurvivesTheRoundTrip() throws IOException {
        String text = "line one\nline two | with a pipe, \"quotes\" and ünïcödé ☔";
        client.post("tester", text, "Somewhere");

        assertEquals(text, client.fetchAll().get(0).text());
        // and it's still readable after a "restart"
        assertEquals(text, new CommentStore(file).loadAll().get(0).text());
    }

    @Test
    void blankCommentsAreRejected() {
        IOException error = assertThrows(IOException.class, () -> client.post("tester", "   ", "Somewhere"));
        assertTrue(error.getMessage().contains("can't be empty"), error.getMessage());
    }

    @Test
    void blankLocationGetsADefault() throws IOException {
        client.post("tester", "hello", "");
        assertEquals("Unknown location", client.fetchAll().get(0).location());
    }

    @Test
    void emptyBoardIsJustEmpty() throws IOException {
        assertTrue(client.fetchAll().isEmpty());
    }
}
