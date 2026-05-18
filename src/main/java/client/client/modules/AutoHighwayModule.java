package client.client.modules;

import client.Minecraft;
import client.client.module.Module;
import client.net.SocketClient;
import global.Packets;

public class AutoHighwayModule extends Module {

    // Highway is 3 wide, obsidian floor + 2 high walls cleared
    private static final int BLOCK_ID = 4; // Obsidian
    private static final int WIDTH    = 3; // blocks wide
    private int tickDelay = 0;

    public AutoHighwayModule() {
        super("AutoHighway", "Builds obsidian highway as you walk", Category.PLAYER);
        setKeybind(org.lwjgl.input.Keyboard.KEY_H);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.mc;
        if (mc == null || mc.localPlayer == null || mc.level == null) return;
        if (mc.chat != null && mc.chat.toggled) return;

        // Run every 2 ticks
        tickDelay++;
        if (tickDelay < 2) return;
        tickDelay = 0;

        int px = (int) Math.floor(mc.localPlayer.x);
        int py = (int) Math.floor(mc.localPlayer.y);
        int pz = (int) Math.floor(mc.localPlayer.z);

        // Figure out which axis we're moving on
        float yaw = ((mc.localPlayer.yRotation % 360) + 360) % 360;
        int dirX = 0, dirZ = 0;
        if      (yaw < 45 || yaw >= 315) { dirZ =  1; } // South
        else if (yaw < 135)               { dirX = -1; } // West
        else if (yaw < 225)               { dirZ = -1; } // North
        else                              { dirX =  1; } // East

        // Perpendicular axis for width
        int perpX = dirZ;
        int perpZ = dirX;

        int half = WIDTH / 2;

        for (int w = -half; w <= half; w++) {
            int bx = px + perpX * w;
            int bz = pz + perpZ * w;

            // Floor block (y-1)
            placeIfAir(mc, bx, py - 1, bz, BLOCK_ID);

            // Clear 2 blocks above floor (player height)
            breakIfSolid(mc, bx, py,     bz);
            breakIfSolid(mc, bx, py + 1, bz);
        }
    }

    private void placeIfAir(Minecraft mc, int x, int y, int z, int id) {
        if (!mc.level.isSolidTile(x, y, z)) {
            try {
                mc.level.setTile(x, y, z, id);
                SocketClient.sendBlock(Packets.BLOCK_PLACE, x, y, z, id);
            } catch (Exception e) { }
        }
    }

    private void breakIfSolid(Minecraft mc, int x, int y, int z) {
        if (mc.level.isSolidTile(x, y, z)) {
            try {
                mc.level.setTile(x, y, z, 0);
                SocketClient.sendBlock(Packets.BLOCK_BREAK, x, y, z);
            } catch (Exception e) { }
        }
    }
}
