package client.client.gui;

import client.FontRenderer;
import client.client.module.Module;
import client.client.module.ModuleManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.Color;
import java.util.*;

import static org.lwjgl.opengl.GL11.*;

public class ClickGUI {

    private boolean open = false;
    private final ModuleManager manager;
    private final FontRenderer font;

    private static final int PANEL_W   = 170;
    private static final int HEADER_H  = 22;
    private static final int ROW_H     = 20;
    private static final int PANEL_GAP = 10;
    private static final int MARGIN    = 12;

    private final Map<Module.Category, int[]>   panelPos  = new LinkedHashMap<>();
    private final Map<Module.Category, Boolean> collapsed = new HashMap<>();

    private Module.Category dragging = null;
    private int dragOffX, dragOffY;

    private Module binding = null;

    private boolean prevLeft  = false;
    private boolean prevRight = false;

    public ClickGUI(ModuleManager manager, FontRenderer font) {
        this.manager = manager;
        this.font    = font;

        int x = MARGIN;
        for (Module.Category cat : Module.Category.values()) {
            panelPos.put(cat, new int[]{x, 40});
            collapsed.put(cat, false);
            x += PANEL_W + PANEL_GAP;
        }
    }

    public boolean isOpen() { return open; }

    public void toggle() {
        open = !open;
        if (open) {
            Mouse.setGrabbed(false);
            prevLeft  = true;
            prevRight = true;
        } else {
            Mouse.setGrabbed(true);
            binding = null;
        }
    }

    public void render(int sw, int sh) {
        if (!open) return;

        int mx = Mouse.getX();
        int my = sh - Mouse.getY() - 1;

        boolean ldown  = Mouse.isButtonDown(0);
        boolean rdown  = Mouse.isButtonDown(1);
        boolean lclick = ldown  && !prevLeft;
        boolean rclick = rdown  && !prevRight;

        if (!ldown) dragging = null;
        if (dragging != null) {
            int[] pos = panelPos.get(dragging);
            pos[0] = mx - dragOffX;
            pos[1] = my - dragOffY;
        }

        setupOrtho(sw, sh);
        drawRect(0, 0, sw, sh, new Color(0, 0, 0, 100));

        for (Module.Category cat : Module.Category.values()) {
            renderPanel(cat, mx, my, lclick, rclick, ldown);
        }

        if (binding != null) {
            String msg = "Press a key to bind [" + binding.getName() + "]  (ESC = clear)";
            int tw = font.getStringWidth(msg);
            int th = font.getStringHeight();
            int tx = (sw - tw) / 2;
            int ty = sh - 36;
            drawRect(tx - 8, ty - 5, tx + tw + 8, ty + th + 5, new Color(15, 15, 20, 220));
            drawRect(tx - 8, ty - 5, tx + tw + 8, ty - 3, new Color(255, 200, 50, 255));
            glEnable(GL_TEXTURE_2D);
            font.drawString(msg, tx, ty, new Color(255, 220, 80), true);
            glDisable(GL_TEXTURE_2D);
        }

        restoreOrtho();

        prevLeft  = ldown;
        prevRight = rdown;
    }

    private void renderPanel(Module.Category cat, int mx, int my,
                              boolean lclick, boolean rclick, boolean ldown) {
        int[]   pos  = panelPos.get(cat);
        int     px   = pos[0], py = pos[1];
        boolean coll = collapsed.get(cat);

        List<Module> mods = new ArrayList<>();
        for (Module m : manager.getModules())
            if (m.getCategory() == cat) mods.add(m);

        int panelH = HEADER_H + (coll ? 0 : mods.size() * ROW_H) + 2;

        drawRect(px + 4, py + 4, px + PANEL_W + 4, py + panelH + 4, new Color(0, 0, 0, 70));
        drawRect(px, py, px + PANEL_W, py + panelH, new Color(16, 16, 20, 235));

        Color hc   = cat.color;
        Color hDark = new Color(hc.getRed()/4, hc.getGreen()/4, hc.getBlue()/4, 245);
        drawRect(px, py, px + PANEL_W, py + HEADER_H, hDark);
        drawRect(px, py, px + PANEL_W, py + 2, hc);

        int th = font.getStringHeight();
        glEnable(GL_TEXTURE_2D);
        font.drawString(cat.label.toUpperCase(), px + 8, py + (HEADER_H - th) / 2, hc, true);

        String arrow = coll ? ">" : "v";
        int aw = font.getStringWidth(arrow);
        font.drawString(arrow, px + PANEL_W - aw - 8, py + (HEADER_H - th) / 2, new Color(180, 180, 190), true);
        glDisable(GL_TEXTURE_2D);

        boolean headerHovered = inRect(mx, my, px, py, PANEL_W, HEADER_H);

        if (headerHovered && ldown && dragging == null && !lclick) {
            dragging = cat;
            dragOffX = mx - px;
            dragOffY = my - py;
        }
        if (headerHovered && rclick) {
            collapsed.put(cat, !coll);
        }

        if (!coll) {
            for (int i = 0; i < mods.size(); i++) {
                Module m  = mods.get(i);
                int    ry = py + HEADER_H + i * ROW_H;
                boolean hovered = inRect(mx, my, px, ry, PANEL_W, ROW_H);

                Color rowBg = hovered
                        ? new Color(45, 45, 58, 230)
                        : (i % 2 == 0 ? new Color(20, 20, 26, 215) : new Color(24, 24, 30, 215));
                drawRect(px, ry, px + PANEL_W, ry + ROW_H, rowBg);

                if (m.isEnabled()) drawRect(px, ry, px + 3, ry + ROW_H, hc);

                glEnable(GL_TEXTURE_2D);
                Color nameCol = m.isEnabled() ? Color.WHITE : new Color(130, 130, 140);
                font.drawString(m.getName(), px + 10, ry + (ROW_H - th) / 2, nameCol, true);

                // Show gear icon for modules with submenus
                boolean hasSubmenu = m.getName().equals("Scaffold") ||
                                     m.getName().equals("SpamPlace") ||
                                     m.getName().equals("Annoy");
                String right = hasSubmenu ? "[>]" : (m.getKeybind() >= 0 ? Keyboard.getKeyName(m.getKeybind()) : "-");
                Color rightCol = hasSubmenu ? new Color(255,200,80) : (m == binding ? new Color(255,220,80) : new Color(90,90,110));
                int kw = font.getStringWidth(right);
                font.drawString(right, px + PANEL_W - kw - 8, ry + (ROW_H - th) / 2, rightCol, true);
                glDisable(GL_TEXTURE_2D);

                if (hovered && lclick && dragging == null) m.toggle();
                if (hovered && rclick) {
                    if (m.getName().equals("Scaffold")) {
                        client.client.modules.ScaffoldModule.openSubmenu();
                    } else if (m.getName().equals("Annoy")) {
                        client.client.modules.AnnoyModule.openSubmenu();
                    } else if (m.getName().equals("SpamPlace")) {
                        client.client.modules.SpamPlaceModule.openSubmenu();
                    } else {
                        binding = (binding == m) ? null : m;
                    }
                }
            }
        }

        drawRect(px, py + panelH - 2, px + PANEL_W, py + panelH,
                new Color(hc.getRed(), hc.getGreen(), hc.getBlue(), 60));
    }

    public void handleKey(int key) {
        if (binding != null) {
            if (key == Keyboard.KEY_ESCAPE) binding.setKeybind(-1);
            else binding.setKeybind(key);
            binding = null;
            return;
        }
        if (key == Keyboard.KEY_ESCAPE || key == Keyboard.KEY_RSHIFT) toggle();
    }

    private void setupOrtho(int w, int h) {
        glMatrixMode(GL_PROJECTION); glPushMatrix(); glLoadIdentity();
        glOrtho(0, w, h, 0, -1, 1);
        glMatrixMode(GL_MODELVIEW); glPushMatrix(); glLoadIdentity();
        glDisable(GL_DEPTH_TEST); glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND); glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_TEXTURE_2D);
    }

    private void restoreOrtho() {
        glEnable(GL_DEPTH_TEST); glEnable(GL_CULL_FACE); glDisable(GL_BLEND);
        glPopMatrix(); glMatrixMode(GL_PROJECTION); glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
    }

    private void drawRect(int x1, int y1, int x2, int y2, Color c) {
        glColor4f(c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f, c.getAlpha()/255f);
        glBegin(GL_QUADS);
        glVertex2f(x1,y1); glVertex2f(x2,y1); glVertex2f(x2,y2); glVertex2f(x1,y2);
        glEnd();
    }

    private boolean inRect(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x+w && my >= y && my < y+h;
    }
}
