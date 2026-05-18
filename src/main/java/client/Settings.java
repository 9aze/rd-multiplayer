package client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public final class Settings {

    private static final Path PATH = Paths.get("settings.properties");
    private static float gamma = 0.5f;
    private static int renderDistance = 8;
    private static boolean loaded = false;

    private Settings() {}

    public static synchronized float getGamma() {
        ensureLoaded();
        return gamma;
    }

    public static synchronized void setGamma(float value) {
        ensureLoaded();
        gamma = clamp(value, 0f, 1f);
        save();
    }

    public static synchronized int getRenderDistance() {
        ensureLoaded();
        return renderDistance;
    }

    public static synchronized void setRenderDistance(int value) {
        ensureLoaded();
        renderDistance = Math.max(2, Math.min(32, value));
        save();
    }

    public static float gammaMultiplier() {
        float g = getGamma();
        return 0.4f + g * 1.2f;
    }

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        if (!Files.exists(PATH)) return;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(PATH)) {
            p.load(in);
        } catch (IOException e) {
            System.err.println("Settings: failed to read " + PATH + ": " + e.getMessage());
            return;
        }
        gamma = parseFloat(p.getProperty("gamma"), gamma);
        renderDistance = parseInt(p.getProperty("render_distance"), renderDistance);
        gamma = clamp(gamma, 0f, 1f);
        renderDistance = Math.max(2, Math.min(32, renderDistance));
    }

    private static void save() {
        Properties p = new Properties();
        p.setProperty("gamma", Float.toString(gamma));
        p.setProperty("render_distance", Integer.toString(renderDistance));
        try (OutputStream out = Files.newOutputStream(PATH)) {
            p.store(out, "rd-multiplayer options");
        } catch (IOException e) {
            System.err.println("Settings: failed to write " + PATH + ": " + e.getMessage());
        }
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static float parseFloat(String s, float fallback) {
        if (s == null) return fallback;
        try { return Float.parseFloat(s); } catch (NumberFormatException e) { return fallback; }
    }

    private static int parseInt(String s, int fallback) {
        if (s == null) return fallback;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return fallback; }
    }
}