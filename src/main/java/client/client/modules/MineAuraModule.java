package client.client.modules;

import client.Minecraft;
import client.client.module.Module;
import client.net.SocketClient;
import client.player.remote.RemotePlayer;
import global.Packets;

import java.util.Map;

public class MineAuraModule extends Module {

    private static final double TRIGGER_RANGE = 8.0; // must be within 8 blocks
    private static final double REACH         = 9.5; // stay under server's 10 block limit
    private int tickDelay = 0;

    public MineAuraModule() {
        super("MineAura", "Mines under nearby players so they fall into void", Category.PLAYER);
        setKeybind(org.lwjgl.input.Keyboard.KEY_U);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.mc;
        if (mc == null || mc.localPlayer == null || mc.level == null) return;
        if (mc.playerManager == null) return;
        if (mc.chat != null && mc.chat.toggled) return;

        // Throttle to every 2 ticks so we don't spam too fast
        tickDelay++;
        if (tickDelay < 2) return;
        tickDelay = 0;

        Map<String, RemotePlayer> players = mc.playerManager.getPlayers();
        if (players == null || players.isEmpty()) return;

        for (RemotePlayer rp : players.values()) {
            double dx = rp.x - mc.localPlayer.x;
            double dy = rp.y - mc.localPlayer.y;
            double dz = rp.z - mc.localPlayer.z;
            double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);

            if (dist > TRIGGER_RANGE) continue;

            // Mine a 3x3 area under the player's feet, going down 2 layers
            int px = (int) Math.floor(rp.x);
            int pz = (int) Math.floor(rp.z);
            int py = (int) Math.floor(rp.y) - 1; // block under their feet

            for (int layer = 0; layer >= -1; layer--) {
                for (int bx = px - 1; bx <= px + 1; bx++) {
                    for (int bz = pz - 1; bz <= pz + 1; bz++) {
                        int by = py + layer;

                        // Check we're within reach of this block
                        double bdx = bx + 0.5 - mc.localPlayer.x;
                        double bdy = by + 0.5 - mc.localPlayer.y;
                        double bdz = bz + 0.5 - mc.localPlayer.z;
                        double bdist = Math.sqrt(bdx*bdx + bdy*bdy + bdz*bdz);
                        if (bdist > REACH) continue;

                        // Only break solid blocks
                        if (!mc.level.isSolidTile(bx, by, bz)) continue;

                        try {
                            mc.level.setTile(bx, by, bz, 0);
                            SocketClient.sendBlock(Packets.BLOCK_BREAK, bx, by, bz);
                        } catch (Exception e) {
                            // ignore
                        }
                    }
                }
            }
        }
    }
}
