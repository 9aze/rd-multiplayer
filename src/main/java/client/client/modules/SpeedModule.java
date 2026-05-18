package client.client.modules;

import client.Minecraft;
import client.client.module.Module;

public class SpeedModule extends Module {
    // Server allows 20 blocks/sec. Normal sprint = ~0.08 * 60 = 4.8 blocks/sec.
    // We can safely go up to ~1.8x before the anticheat kicks in.
    public static float speedMultiplier = 1.6f;

    public SpeedModule() {
        super("Speed", "Move faster", Category.MOVEMENT);
        setKeybind(org.lwjgl.input.Keyboard.KEY_V);
    }

    @Override public void onEnable() { }
    @Override public void onDisable() { }
}
