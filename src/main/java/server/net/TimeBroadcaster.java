package server.net;

import global.Packets;
import server.Server;
import server.client.Client;
import server.level.WorldTime;

public final class TimeBroadcaster {
    private static final long REBROADCAST_MS = 30000L;
    private TimeBroadcaster() {}

    public static void start() {
        Thread t = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(REBROADCAST_MS);
                } catch (InterruptedException e) {
                    return;
                }
                broadcastAll();
            }
        }, "TimeBroadcaster");
        t.setDaemon(true);
        t.start();
    }

    public static void broadcastAll() {
        for (Client c : Server.clients) sendTo(c);
    }

    public static void sendTo(Client client) {
        client.send(o -> {
            o.writeByte(Packets.TIME_OF_DAY);
            o.writeLong(WorldTime.EPOCH_MILLIS);
            o.writeLong(WorldTime.CYCLE_LENGTH_MS);
            o.writeLong(System.currentTimeMillis());
        });
    }
}