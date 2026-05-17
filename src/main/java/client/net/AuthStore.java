package client.net;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class AuthStore {

    private static final Path PATH = Paths.get(".auth");

    private AuthStore() {}

    /** Identifies a row in the .auth file. */
    private static final class Entry {
        String server;
        String username;
        String token;

        Entry(String server, String username, String token) {
            this.server = server;
            this.username = username;
            this.token = token;
        }
    }

    public static String getToken(String server, String username) {
        for (Entry e : readAll()) {
            if (e.server.equals(server) && e.username.equalsIgnoreCase(username)) {
                return e.token;
            }
        }
        return null;
    }

    public static void saveToken(String server, String username, String token) {
        List<Entry> rows = readAll();
        boolean replaced = false;
        for (Entry e : rows) {
            if (e.server.equals(server) && e.username.equalsIgnoreCase(username)) {
                e.token = token;
                replaced = true;
                break;
            }
        }
        if (!replaced) rows.add(new Entry(server, username, token));
        writeAll(rows);
    }

    private static List<Entry> readAll() {
        List<Entry> rows = new ArrayList<>();
        if (!Files.exists(PATH)) return rows;
        try {
            for (String line : Files.readAllLines(PATH, StandardCharsets.UTF_8)) {
                if (line.isEmpty()) continue;
                String[] parts = line.split(",", -1);
                if (parts.length < 3) continue;
                rows.add(new Entry(parts[0], parts[1], parts[2]));
            }
        } catch (IOException e) {
            System.err.println("Failed to read .auth: " + e.getMessage());
        }
        return rows;
    }

    private static void writeAll(List<Entry> rows) {
        StringBuilder sb = new StringBuilder();
        for (Entry e : rows) {
            sb.append(e.server).append(',')
              .append(e.username).append(',')
              .append(e.token).append('\n');
        }
        try {
            Files.write(PATH, sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("Failed to write .auth: " + e.getMessage());
        }
    }
}