package client.client.modules;

import client.client.module.Module;

public class AutoSprintModule extends Module {
    public AutoSprintModule() {
        super("AutoSprint", "Always sprint automatically", Category.MOVEMENT);
        setKeybind(org.lwjgl.input.Keyboard.KEY_J);
    }
}
