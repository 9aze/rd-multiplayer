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
    private final FontRenderer  font;

    // Layout
    private static final int PANEL_W  = 180;
    private static final int HEADER_H = 28;
    private static final int ROW_H    = 22;
    private static final int GAP      = 8;
    private static final int MARGIN   = 14;

    private final Map<Module.Category, int[]>   pos       = new LinkedHashMap<>();
    private final Map<Module.Category, Boolean> collapsed = new HashMap<>();

    // Drag
    private Module.Category dragging = null;
    private int dragOffX, dragOffY;

    // Keybind
    private Module binding = null;

    // Click debounce
    private boolean prevL = false, prevR = false;

    // Configurable modules
    private static final Set<String> HAS_SUBMENU = new HashSet<>(Arrays.asList(
        "Scaffold", "SpamPlace", "Annoy"
    ));

    public ClickGUI(ModuleManager manager, FontRenderer font) {
        this.manager = manager;
        this.font    = font;
        int x = MARGIN;
        for (Module.Category cat : Module.Category.values()) {
            pos.put(cat, new int[]{x, 36});
            collapsed.put(cat, false);
            x += PANEL_W + GAP;
        }
    }

    public boolean isOpen() { return open; }

    public void toggle() {
        open = !open;
        if (open) { Mouse.setGrabbed(false); prevL = prevR = true; }
        else       { Mouse.setGrabbed(true);  binding = null; }
    }

    public void render(int sw, int sh) {
        if (!open) return;

        int mx = Mouse.getX();
        int my = sh - Mouse.getY() - 1;

        boolean ldown  = Mouse.isButtonDown(0);
        boolean rdown  = Mouse.isButtonDown(1);
        boolean lclick = ldown  && !prevL;
        boolean rclick = rdown  && !prevR;

        if (!ldown) dragging = null;
        if (dragging != null) {
            int[] p = pos.get(dragging);
            p[0] = mx - dragOffX;
            p[1] = my - dragOffY;
        }

        setupOrtho(sw, sh);

        // Full-screen tinted backdrop
        drawRect(0, 0, sw, sh, new Color(0, 0, 0, 120));

        // Watermark top-left
        glEnable(GL_TEXTURE_2D);
        font.drawString("Qyro Client", 8, 8, new Color(255, 255, 255, 180), true);
        glDisable(GL_TEXTURE_2D);

        for (Module.Category cat : Module.Category.values())
            renderPanel(cat, mx, my, lclick, rclick, ldown, sh);

        // Keybind overlay
        if (binding != null) {
            String msg = "Press key for [" + binding.getName() + "]   ESC = clear";
            int tw = font.getStringWidth(msg), th = font.getStringHeight();
            int tx = (sw - tw) / 2, ty = sh - 40;
            drawRect(tx-10, ty-6, tx+tw+10, ty+th+6, new Color(10,10,15,230));
            drawRect(tx-10, ty-6, tx+tw+10, ty-4,    new Color(255,180,40,255));
            glEnable(GL_TEXTURE_2D);
            font.drawString(msg, tx, ty, new Color(255,210,60), true);
            glDisable(GL_TEXTURE_2D);
        }

        restoreOrtho();
        prevL = ldown; prevR = rdown;
    }

    private void renderPanel(Module.Category cat, int mx, int my,
                              boolean lclick, boolean rclick, boolean ldown, int sh) {
        int[]   p    = pos.get(cat);
        int     px   = p[0], py = p[1];
        boolean coll = collapsed.get(cat);
        Color   hc   = cat.color;

        List<Module> mods = new ArrayList<>();
        for (Module m : manager.getModules())
            if (m.getCategory() == cat) mods.add(m);

        int rows   = coll ? 0 : mods.size();
        int panelH = HEADER_H + rows * ROW_H + 2;

        // Drop shadow
        drawRect(px+3, py+3, px+PANEL_W+3, py+panelH+3, new Color(0,0,0,60));

        // Panel body
        drawRect(px, py, px+PANEL_W, py+panelH, new Color(14,14,18,245));

        // Header gradient (dark tint of category color)
        drawRect(px, py, px+PANEL_W, py+HEADER_H,
            new Color(hc.getRed()/5, hc.getGreen()/5, hc.getBlue()/5, 250));

        // Header top border (full color)
        drawRect(px, py, px+PANEL_W, py+3, hc);

        // Header text
        int th = font.getStringHeight();
        glEnable(GL_TEXTURE_2D);
        font.drawString(cat.label.toUpperCase(),
            px+10, py+(HEADER_H-th)/2, hc, true);

        // Collapse arrow
        String arr = coll ? "▶" : "▼";
        font.drawString(arr,
            px+PANEL_W - font.getStringWidth(arr) - 8,
            py+(HEADER_H-th)/2,
            new Color(160,160,180), true);
        glDisable(GL_TEXTURE_2D);

        // Header interaction
        boolean hHead = inRect(mx,my,px,py,PANEL_W,HEADER_H);
        if (hHead && ldown && dragging==null && !lclick) {
            dragging=cat; dragOffX=mx-px; dragOffY=my-py;
        }
        if (hHead && rclick) collapsed.put(cat, !coll);

        if (!coll) {
            for (int i = 0; i < mods.size(); i++) {
                Module m  = mods.get(i);
                int    ry = py + HEADER_H + i*ROW_H;
                boolean hov = inRect(mx,my,px,ry,PANEL_W,ROW_H);
                boolean on  = m.isEnabled();

                // Row bg — subtle hover
                Color rowBg = hov
                    ? new Color(35,35,45,240)
                    : (i%2==0 ? new Color(18,18,23,230) : new Color(22,22,28,230));
                drawRect(px, ry, px+PANEL_W, ry+ROW_H, rowBg);

                // Enabled indicator — left glow bar
                if (on) {
                    drawRect(px, ry, px+3, ry+ROW_H, hc);
                    // subtle row tint when on
                    drawRect(px+3, ry, px+PANEL_W, ry+ROW_H,
                        new Color(hc.getRed(), hc.getGreen(), hc.getBlue(), 18));
                }

                // Module name
                glEnable(GL_TEXTURE_2D);
                Color nameCol = on ? Color.WHITE : new Color(110,110,125);
                font.drawString(m.getName(), px+10, ry+(ROW_H-th)/2, nameCol, true);

                // Right label — [>] for submenu, key for bind, - for none
                boolean hasSub = HAS_SUBMENU.contains(m.getName());
                String  right  = hasSub ? "[>]"
                               : m == binding ? "..."
                               : m.getKeybind()>=0 ? Keyboard.getKeyName(m.getKeybind())
                               : "-";
                Color rightCol = hasSub ? new Color(255,200,60)
                               : m==binding ? new Color(255,220,80)
                               : new Color(70,70,90);
                font.drawString(right,
                    px+PANEL_W - font.getStringWidth(right) - 8,
                    ry+(ROW_H-th)/2, rightCol, true);
                glDisable(GL_TEXTURE_2D);

                // Separator line
                drawRect(px+6, ry+ROW_H-1, px+PANEL_W-6, ry+ROW_H,
                    new Color(255,255,255,12));

                // Click handlers
                if (hov && lclick && dragging==null) m.toggle();
                if (hov && rclick) {
                    if (hasSub) openSubmenu(m.getName());
                    else        binding = (binding==m) ? null : m;
                }
            }
        }

        // Panel bottom border
        drawRect(px, py+panelH-1, px+PANEL_W, py+panelH,
            new Color(hc.getRed(), hc.getGreen(), hc.getBlue(), 40));
    }

    private void openSubmenu(String name) {
        switch (name) {
            case "Scaffold":  client.client.modules.ScaffoldModule.openSubmenu();  break;
            case "SpamPlace": client.client.modules.SpamPlaceModule.openSubmenu(); break;
            case "Annoy":     client.client.modules.AnnoyModule.openSubmenu();     break;
        }
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

    // ── GL ───────────────────────────────────────────────────────────────────

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

    private void drawRect(int x1,int y1,int x2,int y2,Color c) {
        glColor4f(c.getRed()/255f,c.getGreen()/255f,c.getBlue()/255f,c.getAlpha()/255f);
        glBegin(GL_QUADS);
        glVertex2f(x1,y1); glVertex2f(x2,y1); glVertex2f(x2,y2); glVertex2f(x1,y2);
        glEnd();
    }

    private boolean inRect(int mx,int my,int x,int y,int w,int h) {
        return mx>=x && mx<x+w && my>=y && my<y+h;
    }
}
