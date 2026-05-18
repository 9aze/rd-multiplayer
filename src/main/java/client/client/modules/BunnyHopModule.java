package client.client.modules;

import client.Minecraft;
import client.client.module.Module;

public class BunnyHopModule extends Module {
    public BunnyHopModule() {
        super("BunnyHop", "Auto jump when landing to keep speed", Category.MOVEMENT);
        setKeybind(org.lwjgl.input.Keyboard.KEY_B);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.mc;
        if (mc == null || mc.localPlayer == null) return;
        if (mc.chat != null && mc.chat.toggled) return;

        // Auto jump the moment we touch the ground
        if (mc.localPlayer.onGround) {
            mc.localPlayer.motionY = 0.12F;
        }
    }
}
