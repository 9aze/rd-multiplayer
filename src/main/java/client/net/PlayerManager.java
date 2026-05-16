package client.net;

import client.Position;

import java.util.HashMap;
import java.util.Map;

public class PlayerManager {

    public Map<String, Position> players = new HashMap<>();

    public synchronized void updatePlayer(String username, double x, double y, double z,
                                          float yaw, float pitch, int ping) {
        Position p = players.get(username);
        if (p == null) {
            p = new Position(x, y, z, yaw, ping);
            players.put(username, p);
        }
        p.x = x; p.y = y; p.z = z;
        p.yaw = yaw; p.pitch = pitch; p.ping = ping;
    }

    public synchronized void setPendingSkin(String username, byte[] png) {
        Position p = players.get(username);
        if (p == null) {
            p = new Position(0, 0, 0, 0f, 0);
            players.put(username, p);
        }
        p.pendingSkinPng = png;
    }

    public synchronized void removePlayer(String username) {
        players.remove(username);
    }

    public Position getPlayer(String username) {
        return players.get(username);
    }

    public Map<String, Position> getPlayers() { return players; }
}