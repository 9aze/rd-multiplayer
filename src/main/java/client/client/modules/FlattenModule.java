package client.client.modules;

import client.Minecraft;
import client.client.module.Module;
import client.net.SocketClient;
import global.Packets;

import java.util.ArrayDeque;
import java.util.Deque;

public class FlattenModule extends Module {

    private static final int    RADIUS       = 8;
    private static final double EYE_HEIGHT   = 1.6;
    private static final double MAX_REACH    = 9.0;
    private static final long   MS_PER_BREAK = 220;

    private final Deque<int[]> queue  = new ArrayDeque<>();
    private boolean            queued = false;
    private long               lastBreak = 0;

    public FlattenModule() {
        super("Flatten", "Flattens terrain in radius around you", Category.MISC);
        setKeybind(org.lwjgl.input.Keyboard.KEY_F6);
    }

    @Override public void onEnable()  { queue.clear(); queued = false; }
    @Override public void onDisable() { queue.clear(); queued = false; }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.mc;
        if (mc == null || mc.localPlayer == null || mc.level == null) return;
        if (mc.chat != null && mc.chat.toggled) return;

        if (!queued) {
            int cx = (int) Math.floor(mc.localPlayer.x);
            int cy = (int) Math.floor(mc.localPlayer.y);
            int cz = (int) Math.floor(mc.localPlayer.z);
            // Break all blocks from floor+1 up to floor+5 in radius
            for (int dy = 0; dy <= 4; dy++) {
                for (int dx = -RADIUS; dx <= RADIUS; dx++)
                    for (int dz = -RADIUS; dz <= RADIUS; dz++)
                        queue.add(new int[]{cx+dx, cy+dy, cz+dz});
            }
            queued = true;
        }

        if (queue.isEmpty()) { setEnabled(false); return; }

        long now = System.currentTimeMillis();
        if (now - lastBreak < MS_PER_BREAK) return;

        int checked = 0;
        while (!queue.isEmpty() && checked < 200) {
            int[] b = queue.poll();
            checked++;
            if (!mc.level.isSolidTile(b[0], b[1], b[2])) continue;
            if (!inReach(mc, b[0], b[1], b[2])) { queue.addLast(b); continue; }
            try {
                mc.level.setTile(b[0], b[1], b[2], 0);
                SocketClient.sendBlock(Packets.BLOCK_BREAK, b[0], b[1], b[2]);
                lastBreak = now;
            } catch (Exception e) { }
            return;
        }
    }

    private boolean inReach(Minecraft mc, int bx, int by, int bz) {
        double dx = (bx+0.5) - mc.localPlayer.x;
        double dy = (by+0.5) - (mc.localPlayer.y + EYE_HEIGHT);
        double dz = (bz+0.5) - mc.localPlayer.z;
        return Math.sqrt(dx*dx+dy*dy+dz*dz) <= MAX_REACH;
    }
}
