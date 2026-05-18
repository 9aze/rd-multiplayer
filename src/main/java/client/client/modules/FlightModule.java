package client.client.modules;

import client.Minecraft;
import client.client.module.Module;

public class FlightModule extends Module {
    public static float flySpeed = 0.25f;

    public FlightModule() {
        super("Flight", "Fly freely (overrides vanilla fly)", Category.MOVEMENT);
        setKeybind(org.lwjgl.input.Keyboard.KEY_G);
    }
}
