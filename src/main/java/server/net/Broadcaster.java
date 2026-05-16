package server.net;

import global.Packets;
import server.Server;
import server.client.Client;

import java.io.DataOutputStream;
import java.io.IOException;

public class Broadcaster {

    public static void broadcastBlock(byte packet, int x, int y, int z, int blockId) {
        for (Client c : Server.clients) {
          c.send(o -> {
               o.writeByte(packet);
               o.writeInt(x); o.writeInt(y); o.writeInt(z);
               if (packet == Packets.BLOCK_PLACE) o.writeByte(blockId);
           });
       }
    }

    public static void broadcastBlock(byte packet, int x, int y, int z) {
       broadcastBlock(packet, x, y, z, 0);
    }


    public static void broadcastConnection(int type, Client sender) {
        for (Client client : Server.clients) {
            if (client == sender) continue;
            client.send(o -> {
                o.writeByte(Packets.CONNECTION);
                o.writeInt(type);
                o.writeUTF(sender.getUsername());
            });
        }
    }

    public static void broadcastChat(String author, String message) {
        for (Client client : Server.clients) {
            client.send(o -> {
                o.writeByte(Packets.CHAT);
                o.writeUTF(author);
                o.writeUTF(message);
            });
        }
    }

    public static void broadcastPos(Client sender, double x, double y, double z, float yaw, int ping) {
        for (Client client : Server.clients) {
            if (client == sender) continue;
            client.send(o -> {
                o.writeByte(Packets.POS);
                o.writeUTF(sender.getUsername());
                o.writeDouble(x);
                o.writeDouble(y);
                o.writeDouble(z);
                o.writeFloat(yaw);
                o.writeInt(ping);
            });
        }
    }
}