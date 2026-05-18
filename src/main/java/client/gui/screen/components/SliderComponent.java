package client.gui.screen.components;

public class SliderComponent {
    public String label;
    public int x, y, w, h;
    public float min, max, value;

    public java.util.function.Function<Float, String> formatter;

    public SliderComponent(String label, int x, int y, int w, int h, float min, float max, float value) {
        this.label = label;
        this.x = x; this.y = y; this.w = w; this.h = h;
        this.min = min;
        this.max = max;
        this.value = clamp(value);
    }

    public boolean contains(int mx, int my) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public int knobX() {
        float t = (max == min) ? 0f : (value - min) / (max - min);
        return (int)(x + t * w);
    }

    public void setFromMouse(int mx) {
        if (w <= 0) return;
        float t = (mx - x) / (float) w;
        if (t < 0f) t = 0f;
        if (t > 1f) t = 1f;
        value = min + t * (max - min);
    }

    private float clamp(float v) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }
}