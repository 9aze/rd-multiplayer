package client.client.modules;

import client.Minecraft;
import client.client.module.Module;
import client.net.SocketClient;
import global.Packets;

public class TowerModule extends Module {

    private static final int BLOCK_ID = 4; // Obsidian
    private long lastPlace = 0;

    public TowerModule() {
        super("Tower", "Rapidly builds tower under you", Category.MOVEMENT);
        setKeybind(org.lwjgl.input.Keyboard.KEY_Y);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.mc;
        if (mc == null || mc.localPlayer == null || mc.level == null) return;
        if (mc.chat != null && mc.chat.toggled) return;
        if (!org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_SPACE)) return;

        long now = System.currentTimeMillis();
        if (now - lastPlace < 210) return;
        lastPlace = now;

        int x = (int) Math.floor(mc.localPlayer.x);
        int y = (int) Math.floor(mc.localPlayer.y) - 1;
        int z = (int) Math.floor(mc.localPlayer.z);

        if (!mc.level.isSolidTile(x, y, z)) {
            try {
                mc.level.setTile(x, y, z, BLOCK_ID);
                SocketClient.sendBlock(Packets.BLOCK_PLACE, x, y, z, BLOCK_ID);
                mc.localPlayer.motionY = 0.18F; // boost upward
            } catch (Exception e) { }
        }
    }
}
