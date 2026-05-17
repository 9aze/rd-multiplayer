package server.auth;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class AuthDatabase {
    private static final String DB_URL_PREFIX = "jdbc:sqlite:";
    private static final int TOKEN_BYTES = 32;
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private final String url;
    private final SecureRandom rng = new SecureRandom();
    private final Object lock = new Object();

    public AuthDatabase(String dbPath) {
        this.url = DB_URL_PREFIX + dbPath;
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC driver not on classpath", e);
        }
        init();
    }

    private void init() {
        synchronized (lock) {
            try (Connection c = DriverManager.getConnection(url);
                 Statement s = c.createStatement()) {
                s.execute(
                    "CREATE TABLE IF NOT EXISTS auth_tokens (" +
                    "  username TEXT PRIMARY KEY," +
                    "  token    TEXT NOT NULL," +
                    "  created  INTEGER NOT NULL" +
                    ")"
                );
            } catch (SQLException e) {
                throw new RuntimeException("Failed to initialise auth database", e);
            }
        }
    }

    public String getToken(String username) {
        String key = username.toLowerCase();
        synchronized (lock) {
            try (Connection c = DriverManager.getConnection(url);
                 PreparedStatement ps = c.prepareStatement("SELECT token FROM auth_tokens WHERE username = ?")) {
                ps.setString(1, key);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getString(1);
                    return null;
                }
            } catch (SQLException e) {
                System.err.println("AuthDatabase.getToken failed: " + e.getMessage());
                return null;
            }
        }
    }

    public String registerNewToken(String username) {
        String key = username.toLowerCase();
        String token = generateToken();
        long now = System.currentTimeMillis();
        synchronized (lock) {
            try (Connection c = DriverManager.getConnection(url);
                 PreparedStatement ps = c.prepareStatement("INSERT INTO auth_tokens(username, token, created) VALUES (?, ?, ?)")) {
                ps.setString(1, key);
                ps.setString(2, token);
                ps.setLong  (3, now);
                ps.executeUpdate();
                return token;
            } catch (SQLException e) {
                throw new RuntimeException("Failed to register token for " + username, e);
            }
        }
    }

    public boolean verifyToken(String username, String candidate) {
        if (candidate == null) return false;
        String stored = getToken(username);
        if (stored == null) return false;
        return constantTimeEquals(stored, candidate);
    }
 
    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        rng.nextBytes(bytes);
        char[] out = new char[TOKEN_BYTES * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[i * 2] = HEX[v >>> 4];
            out[i * 2 + 1] = HEX[v & 0x0F];
        }
        return new String(out);
    }
 
    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }
}
