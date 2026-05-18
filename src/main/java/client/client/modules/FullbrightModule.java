package client.client.modules;

import client.client.module.Module;

public class FullbrightModule extends Module {
    public FullbrightModule() {
        super("Fullbright", "Maximum brightness, no darkness", Category.RENDER);
        setKeybind(org.lwjgl.input.Keyboard.KEY_F);
    }
}
