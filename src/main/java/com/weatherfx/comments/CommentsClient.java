package com.weatherfx.comments;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class CommentsClient {

    private static final int TIMEOUT_MS = 4000;

    private final String host;
    private final int port;

    public CommentsClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String address() {
        return host + ":" + port;
    }

    public List<Comment> fetchAll() throws IOException {
        try (Socket socket = connect();
             DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
             DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()))) {
            out.writeUTF("GET");
            out.flush();

            int count = in.readInt();
            List<Comment> comments = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                comments.add(Comment.readFrom(in));
            }
            return comments;
        }
    }

    public void post(String author, String text, String location) throws IOException {
        try (Socket socket = connect();
             DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
             DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()))) {
            out.writeUTF("POST");
            out.writeUTF(author);
            out.writeUTF(text);
            out.writeUTF(location);
            out.flush();

            String reply = in.readUTF();
            if (!"OK".equals(reply)) {
                throw new IOException(reply.startsWith("ERROR ") ? reply.substring(6) : reply);
            }
        }
    }

    private Socket connect() throws IOException {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), TIMEOUT_MS);
            socket.setSoTimeout(TIMEOUT_MS);
            return socket;
        } catch (IOException e) {
            socket.close();
            throw e;
        }
    }
}
