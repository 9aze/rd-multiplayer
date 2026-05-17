package client.gui.screen.components;

public class ButtonComponent {
    public String label;
    public int x, y, w, h;
    public ButtonComponent(String label, int x, int y, int w, int h) {
        this.label = label; this.x = x; this.y = y; this.w = w; this.h = h;
    }
    public boolean contains(int mx, int my) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}