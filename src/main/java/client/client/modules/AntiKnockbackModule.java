package client.client.modules;

import client.client.module.Module;

public class AntiKnockbackModule extends Module {
    public AntiKnockbackModule() {
        super("AntiKnockback", "Reduce knockback from hits", Category.PLAYER);
        setKeybind(org.lwjgl.input.Keyboard.KEY_K);
    }
}
