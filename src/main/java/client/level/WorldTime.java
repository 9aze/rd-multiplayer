package client.level;

public final class WorldTime {

    public static volatile boolean initialized = false;
    public static volatile long epochMillis = 0L;
    public static volatile long cycleLengthMillis = 20L * 60L * 1000L;
    public static volatile long serverClockOffset = 0L;

    public static final float[] FOG_DAY = { 0.69f, 0.81f, 0.92f };
    public static final float[] FOG_NIGHT = { 0.02f, 0.03f, 0.07f };
    public static final float[] SKY_DAY = { 0.50f, 0.80f, 1.00f };
    public static final float[] SKY_NIGHT = { 0.01f, 0.02f, 0.05f };
    public static final float DAY_LIT = 1.0f;
    public static final float DAY_SHADOW = 0.8f;
    public static final float NIGHT_LIT = 0.30f;
    public static final float NIGHT_SHADOW = 0.18f;

    private WorldTime() {}

    public static long now() {
        return System.currentTimeMillis() + serverClockOffset;
    }

    public static double phase() {
        if (!initialized) return 0.0;
        long delta = now() - epochMillis;
        double m = Math.floorMod(delta, cycleLengthMillis);
        return m / (double) cycleLengthMillis;
    }

    public static float skyBrightness() {
        if (!initialized) return 1.0f;
        double t = phase();
        return (float) (0.5 * (1.0 + Math.cos(2.0 * Math.PI * t)));
    }

    public static void mixColor(float[] a, float[] b, float t, float[] out) {
        out[0] = a[0] + (b[0] - a[0]) * t;
        out[1] = a[1] + (b[1] - a[1]) * t;
        out[2] = a[2] + (b[2] - a[2]) * t;
    }

    public static void currentFogColor(float[] out) {
        mixColor(FOG_NIGHT, FOG_DAY, skyBrightness(), out);
    }

    public static void currentSkyColor(float[] out) {
        mixColor(SKY_NIGHT, SKY_DAY, skyBrightness(), out);
    }

    public static float currentLit() {
        return NIGHT_LIT + (DAY_LIT - NIGHT_LIT) * skyBrightness();
    }

    public static float currentShadow() {
        return NIGHT_SHADOW + (DAY_SHADOW - NIGHT_SHADOW) * skyBrightness();
    }
}