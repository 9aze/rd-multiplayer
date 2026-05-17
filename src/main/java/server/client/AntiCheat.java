package server.client;

import server.Server;

public class AntiCheat {

    private static final double EYE_HEIGHT  = 1.6;
    private static final double PLACE_BURST = 5.0;
    private static final double BREAK_BURST = 5.0;

    private AntiCheat() {}

    public static boolean checkBlock(Client client,
                                     int blockX, int blockY, int blockZ,
                                     boolean isPlace, long now) {
        if (!Server.ANTICHEAT) return true;

        double[] pos = client.getLastPos();
        if (pos != null) {
            double dx = (blockX + 0.5) - pos[0];
            double dy = (blockY + 0.5) - (pos[1] + EYE_HEIGHT);
            double dz = (blockZ + 0.5) - pos[2];
            double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
            if (dist > Server.MAX_REACH) {
                if (Server.LOGS) System.out.printf("%s reach violation: %.1f units%n", client.getUsername(), dist);
                return false;
            }
        }

        if (isPlace) {
            if (!consumePlaceToken(client, now)) {
                if (Server.LOGS) System.out.printf("%s place-rate violation%n", client.getUsername());
                return false;
            }
        } else {
            if (!consumeBreakToken(client, now)) {
                if (Server.LOGS) System.out.printf("%s break-rate violation%n", client.getUsername());
                return false;
            }
        }

        return true;
    }

    public static boolean checkMovement(Client client, double x, double y, double z, long now) {
        if (!Server.ANTICHEAT) return true;

        double[] pos = client.getLastPos();
        long lastTime = client.getLastMoveTime();
        double tokens = client.getMoveTokens();

        if (pos == null || lastTime == 0) {
            client.setLastPos(x, y, z, now);
            client.setMoveTokens(Server.MOVE_BURST, now);
            return true;
        }

        long elapsed = now - lastTime;
        if (elapsed > 0) {
            tokens = Math.min(Server.MOVE_BURST, tokens + (elapsed * (Server.MOVE_RATE / 1_000.0)));
        }

        double dx = x - pos[0];
        double dy = y - pos[1];
        double dz = z - pos[2];
        double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);

        if (dist > tokens) {
            if (Server.LOGS) System.out.printf("%s speed violation: tried to move %.2f blocks, only had %.2f tokens remaining%n",
                    client.getUsername(), dist, tokens);
            return false;
        }

        client.setMoveTokens(tokens - dist, now);
        client.setLastPos(x, y, z, now);
        return true;
    }

    private static boolean consumePlaceToken(Client client, long now) {
        long lastRefill = client.getLastPlaceTime();
        double tokens = client.getPlaceTokens();

        if (lastRefill > 0) {
            long elapsed = now - lastRefill;
            tokens = Math.min(PLACE_BURST, tokens + elapsed * (Server.PLACE_RATE / 1_000.0));
        } else {
            tokens = PLACE_BURST;
        }

        if (tokens < 1.0) {
            client.setPlaceTokens(tokens, now);
            return false;
        }

        client.setPlaceTokens(tokens - 1.0, now);
        return true;
    }

    private static boolean consumeBreakToken(Client client, long now) {
        long lastRefill = client.getLastBreakTime();
        double tokens = client.getBreakTokens();

        if (lastRefill > 0) {
            long elapsed = now - lastRefill;
            tokens = Math.min(BREAK_BURST, tokens + elapsed * (Server.BREAK_RATE / 1_000.0));
        } else {
            tokens = BREAK_BURST;
        }

        if (tokens < 1.0) {
            client.setBreakTokens(tokens, now);
            return false;
        }

        client.setBreakTokens(tokens - 1.0, now);
        return true;
    }
}