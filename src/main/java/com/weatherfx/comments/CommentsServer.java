package com.weatherfx.comments;

import com.weatherfx.AppConfig;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// one short connection per request:
//   GET                       -> count, then the comments
//   POST author text location -> "OK" or "ERROR reason"
public class CommentsServer implements Closeable {

    private static final DateTimeFormatter LOG_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ServerSocket serverSocket;
    private final CommentStore store;
    private final ExecutorService pool = Executors.newFixedThreadPool(10);

    public CommentsServer(int port, CommentStore store) throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.store = store;
    }

    public int port() {
        return serverSocket.getLocalPort();
    }

    public void serve() {
        while (!serverSocket.isClosed()) {
            try {
                Socket client = serverSocket.accept();
                pool.execute(() -> handle(client));
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    log("accept failed: " + e.getMessage());
                }
            }
        }
    }

    private void handle(Socket socket) {
        try (socket;
             DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
             DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()))) {
            socket.setSoTimeout(10_000);

            String action = in.readUTF();
            if ("GET".equals(action)) {
                List<Comment> comments = store.loadAll();
                out.writeInt(comments.size());
                for (Comment comment : comments) {
                    comment.writeTo(out);
                }
            } else if ("POST".equals(action)) {
                String author = in.readUTF();
                String text = in.readUTF();
                String location = in.readUTF();
                out.writeUTF(post(author, text, location));
            } else {
                out.writeUTF("ERROR unknown action " + action);
            }
            out.flush();
        } catch (IOException e) {
            log("client " + socket.getRemoteSocketAddress() + " dropped: " + e.getMessage());
        }
    }

    private String post(String author, String text, String location) throws IOException {
        author = author.trim();
        text = text.trim();
        location = location.isBlank() ? "Unknown location" : location.trim();

        if (author.isEmpty() || text.isEmpty()) {
            return "ERROR name and comment can't be empty";
        }
        if (author.length() > Comment.MAX_AUTHOR_LENGTH || text.length() > Comment.MAX_TEXT_LENGTH) {
            return "ERROR comment is too long";
        }

        store.add(new Comment(author, text, location, System.currentTimeMillis()));
        log("@" + author + " posted from " + location);
        return "OK";
    }

    @Override
    public void close() throws IOException {
        serverSocket.close();
        pool.shutdownNow();
    }

    private static void log(String message) {
        System.out.println("[" + LocalTime.now().format(LOG_TIME) + "] " + message);
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : AppConfig.load().commentsPort();
        CommentStore store = new CommentStore(Path.of("comments.jsonl"));

        try (CommentsServer server = new CommentsServer(port, store)) {
            log("comments server listening on port " + server.port() + " (Ctrl+C to stop)");
            server.serve();
        }
    }
}
