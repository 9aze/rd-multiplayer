package client.client.modules;

import client.client.module.Module;

public class NoClipModule extends Module {
    public NoClipModule() {
        super("NoClip", "Walk through walls", Category.MOVEMENT);
        setKeybind(org.lwjgl.input.Keyboard.KEY_N);
    }
}
