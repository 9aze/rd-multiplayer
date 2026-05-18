package client.client.modules;

import client.Minecraft;
import client.client.module.Module;
import client.net.SocketClient;
import client.player.remote.RemotePlayer;
import global.Packets;

import java.util.*;

public class CapsuleAuraModule extends Module {

    private static final int    BLOCK_ID     = 4;   // Obsidian
    private static final double TRIGGER_RANGE = 10.0;
    private static final double EYE_HEIGHT   = 1.6;
    private static final double MAX_REACH    = 9.0;
    private static final long   MS_PER_PLACE = 220;

    // Tracks all blocks we've placed as capsule walls: key = "x,y,z" -> blockId
    private final Map<String, Integer> capsuleBlocks = new HashMap<>();
    // Queue for restoring broken blocks
    private final Queue<int[]> restoreQueue = new ArrayDeque<>();

    private long lastPlace = 0;
    private long lastCapsule = 0;
    private static final long CAPSULE_INTERVAL = 3000; // rebuild capsule every 3s

    public CapsuleAuraModule() {
        super("CapsuleAura", "Traps nearby players in obsidian capsules", Category.PLAYER);
        setKeybind(org.lwjgl.input.Keyboard.KEY_O);
    }

    @Override
    public void onDisable() {
        capsuleBlocks.clear();
        restoreQueue.clear();
    }

    // Called from Minecraft.java when any block is broken
    public void onBlockBroken(int x, int y, int z) {
        String key = x + "," + y + "," + z;
        Integer id = capsuleBlocks.get(key);
        if (id != null) {
            // This was one of our capsule blocks — queue restore
            restoreQueue.add(new int[]{x, y, z, id});
        }
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.mc;
        if (mc == null || mc.localPlayer == null || mc.level == null) return;
        if (mc.playerManager == null) return;
        if (mc.chat != null && mc.chat.toggled) return;

        long now = System.currentTimeMillis();

        // Drain restore queue first (priority)
        if (!restoreQueue.isEmpty() && now - lastPlace >= MS_PER_PLACE) {
            int[] b = restoreQueue.poll();
            if (b != null) tryPlace(mc, b[0], b[1], b[2], b[3]);
            lastPlace = now;
            return;
        }

        // Build/rebuild capsules around nearby players
        if (now - lastCapsule < CAPSULE_INTERVAL) return;
        lastCapsule = now;

        Map<String, RemotePlayer> players = mc.playerManager.getPlayers();
        if (players == null || players.isEmpty()) return;

        for (RemotePlayer rp : players.values()) {
            double dx = rp.x - mc.localPlayer.x;
            double dy = rp.y - mc.localPlayer.y;
            double dz = rp.z - mc.localPlayer.z;
            if (Math.sqrt(dx*dx + dy*dy + dz*dz) > TRIGGER_RANGE) continue;

            buildCapsule(mc, rp);
        }
    }

    private void buildCapsule(Minecraft mc, RemotePlayer rp) {
        int px = (int) Math.floor(rp.x);
        int py = (int) Math.floor(rp.y);
        int pz = (int) Math.floor(rp.z);

        // 3x4x3 capsule shell around player
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = -1; dy <= 3; dy++) {
                    // Only place shell (skip inside)
                    boolean isShell = dx == -1 || dx == 1 || dz == -1 || dz == 1
                            || dy == -1 || dy == 3;
                    if (!isShell) continue;

                    int bx = px + dx, by = py + dy, bz = pz + dz;
                    if (!mc.level.isSolidTile(bx, by, bz)) {
                        String key = bx + "," + by + "," + bz;
                        capsuleBlocks.put(key, BLOCK_ID);
                        restoreQueue.add(new int[]{bx, by, bz, BLOCK_ID});
                    }
                }
            }
        }
    }

    private void tryPlace(Minecraft mc, int x, int y, int z, int id) {
        double dx = (x+0.5) - mc.localPlayer.x;
        double dy = (y+0.5) - (mc.localPlayer.y + EYE_HEIGHT);
        double dz = (z+0.5) - mc.localPlayer.z;
        if (Math.sqrt(dx*dx+dy*dy+dz*dz) > MAX_REACH) return;
        if (mc.level.isSolidTile(x, y, z)) return;
        try {
            mc.level.setTile(x, y, z, id);
            SocketClient.sendBlock(Packets.BLOCK_PLACE, x, y, z, id);
        } catch (Exception e) { }
    }
}
