package client.client.module;

import java.awt.Color;

public abstract class Module {
    private final String name;
    private final String description;
    private final Category category;
    private boolean enabled = false;
    private int keybind = -1;

    public enum Category {
        MOVEMENT("Movement", new Color(100, 200, 255)),
        RENDER("Render", new Color(255, 200, 100)),
        PLAYER("Player", new Color(150, 255, 150)),
        MISC("Misc", new Color(200, 150, 255));

        public final String label;
        public final Color color;
        Category(String label, Color color) { this.label = label; this.color = color; }
    }

    public Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick() {}
    public void onRender(int width, int height) {}

    public void toggle() {
        enabled = !enabled;
        if (enabled) onEnable(); else onDisable();
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean e) {
        if (e != enabled) toggle();
    }
    public int getKeybind() { return keybind; }
    public void setKeybind(int k) { keybind = k; }
}
