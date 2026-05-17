package client.gui.screen;

import client.FontRenderer;

public abstract class Screen {
    public Screen() { init(); }
    public abstract void render(FontRenderer font, int width, int height);
    public void init() {}
    public void destroy() {}
}