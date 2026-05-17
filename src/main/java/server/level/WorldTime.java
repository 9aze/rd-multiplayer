package server.level;

public final class WorldTime {
    public static final long DAY_LENGTH_MS = 10L * 60L * 1000L;
    public static final long NIGHT_LENGTH_MS = 10L * 60L * 1000L;
    public static final long CYCLE_LENGTH_MS = DAY_LENGTH_MS + NIGHT_LENGTH_MS;
    public static final long EPOCH_MILLIS = System.currentTimeMillis();

    private WorldTime() {}

    public static double phase(long nowMillis) {
        long delta = nowMillis - EPOCH_MILLIS;
        double m = Math.floorMod(delta, CYCLE_LENGTH_MS);
        return m / (double) CYCLE_LENGTH_MS;
    }

    public static double skyBrightness(long nowMillis) {
        double t = phase(nowMillis);
        return 0.5 * (1.0 + Math.cos(2.0 * Math.PI * t));
    }
}