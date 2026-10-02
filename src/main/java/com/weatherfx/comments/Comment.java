package com.weatherfx.comments;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * One comment on the board.
 *
 * @param postedAt epoch millis, stamped by the server so everyone agrees on the order
 */
public record Comment(String author, String text, String location, long postedAt) {

    public static final int MAX_AUTHOR_LENGTH = 30;
    public static final int MAX_TEXT_LENGTH = 500;

    void writeTo(DataOutputStream out) throws IOException {
        out.writeUTF(author);
        out.writeUTF(text);
        out.writeUTF(location);
        out.writeLong(postedAt);
    }

    static Comment readFrom(DataInputStream in) throws IOException {
        return new Comment(in.readUTF(), in.readUTF(), in.readUTF(), in.readLong());
    }
}
