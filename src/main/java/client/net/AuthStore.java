package client.net;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public final class AuthStore {

    private static final Path PATH = Paths.get(".auth");
    private static final Path LOCK_PATH = Paths.get(".auth.lock");

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
        List<Entry> rows = readAllOrNull();
        if (rows == null) return null; // read failed
        for (Entry e : rows) {
            if (e.server.equals(server) && e.username.equalsIgnoreCase(username)) {
                return e.token;
            }
        }
        return null;
    }

    public static void saveToken(String server, String username, String token) {
        try (RandomAccessFile lockFile = new RandomAccessFile(LOCK_PATH.toFile(), "rw");
             FileChannel ch = lockFile.getChannel();
             FileLock ignored = ch.lock()) {

            List<Entry> rows = readAllOrNull();
            if (rows == null) {
                System.err.println("AuthStore: refusing to save token, .auth read failed earlier");
                return;
            }

            boolean replaced = false;
            for (Entry e : rows) {
                if (e.server.equals(server) && e.username.equalsIgnoreCase(username)) {
                    e.token = token;
                    replaced = true;
                    break;
                }
            }
            if (!replaced) rows.add(new Entry(server, username, token));

            writeAllAtomic(rows);
        } catch (IOException e) {
            System.err.println("AuthStore.saveToken failed: " + e.getMessage());
        }
    }

    private static List<Entry> readAllOrNull() {
        if (!Files.exists(PATH)) return new ArrayList<>();
        List<String> lines;
        try {
            lines = Files.readAllLines(PATH, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Failed to read .auth: " + e.getMessage());
            return null; // distinguish failure from empty
        }
        List<Entry> rows = new ArrayList<>(lines.size());
        for (String line : lines) {
            if (line.isEmpty()) continue;
            String[] parts = line.split(",", -1);
            if (parts.length < 3) continue;
            rows.add(new Entry(parts[0], parts[1], parts[2]));
        }
        return rows;
    }

    private static void writeAllAtomic(List<Entry> rows) {
        StringBuilder sb = new StringBuilder();
        for (Entry e : rows) {
            sb.append(e.server).append(',')
              .append(e.username).append(',')
              .append(e.token).append('\n');
        }
        Path tmp = PATH.resolveSibling(".auth.tmp");
        try {
            Files.write(tmp, sb.toString().getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(tmp, PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (UnsupportedOperationException | IOException atomicFailed) {
                Files.move(tmp, PATH, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.err.println("Failed to write .auth: " + e.getMessage());
            try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
        }
    }
}