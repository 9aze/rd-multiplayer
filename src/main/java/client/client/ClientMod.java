package client.client;

import client.FontRenderer;
import client.Minecraft;
import client.client.gui.ClickGUI;
import client.client.module.Module;
import client.client.module.ModuleManager;
import client.client.modules.ESPModule;
import client.client.modules.TracersModule;
import client.client.modules.ChamsModule;
import client.client.modules.RadarModule;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

public class ClientMod {
    public static final String NAME    = "RDClient";
    public static final String VERSION = "1.0";
    public static ClientMod INSTANCE;

    public final ModuleManager moduleManager;
    public final ClickGUI      clickGUI;

    private final boolean[] keyWasDown = new boolean[256];

    public ClientMod(FontRenderer font) {
        INSTANCE      = this;
        moduleManager = new ModuleManager();
        clickGUI      = new ClickGUI(moduleManager, font);
    }

    public void onTick() {
        moduleManager.onTick();
    }

    public void onRender(int width, int height) {
        // Feed keyboard events to Annoy submenu if open
        if (client.client.modules.AnnoyModule.submenuOpen) {
            while (Keyboard.next()) {
                if (!Keyboard.getEventKeyState()) continue;
                client.client.modules.AnnoyModule.handleKey(
                    Keyboard.getEventKey(),
                    Keyboard.getEventCharacter());
            }
        } else {
            // Normal key polling
            for (int k = 0; k < keyWasDown.length; k++) {
                boolean down = Keyboard.isKeyDown(k);
                if (down && !keyWasDown[k]) onKeyPressed(k);
                keyWasDown[k] = down;
            }
        }

        // ESP
        Module esp = moduleManager.getByName("ESP");
        if (esp != null && esp.isEnabled() && Minecraft.mc.playerManager != null)
            ESPModule.renderESP(Minecraft.mc.playerManager);

        // Chams
        Module chams = moduleManager.getByName("Chams");
        if (chams != null && chams.isEnabled())
            ChamsModule.renderChams(Minecraft.mc);

        // Tracers
        Module tracers = moduleManager.getByName("Tracers");
        if (tracers != null && tracers.isEnabled())
            TracersModule.renderTracers(width, height);

        // Radar
        Module radar = moduleManager.getByName("Radar");
        if (radar != null && radar.isEnabled())
            RadarModule.renderRadar(width, height);

        if (clickGUI.isOpen()) {
            clickGUI.render(width, height);
        } else {
            renderModuleHUD(width, height);
        }

        moduleManager.onRender(width, height);
    }

    private void onKeyPressed(int key) {
        // Don't fire module keybinds while chat is open
        if (Minecraft.mc != null && Minecraft.mc.chat != null && Minecraft.mc.chat.toggled) return;

        if (clickGUI.isOpen()) {
            clickGUI.handleKey(key);
            return;
        }
        if (key == Keyboard.KEY_RSHIFT) {
            clickGUI.toggle();
            return;
        }
        moduleManager.onKey(key);
    }

    private void renderModuleHUD(int width, int height) {
        FontRenderer font = Minecraft.mc != null ? Minecraft.mc.getClientFont() : null;
        if (font == null) return;

        List<Module> active = new ArrayList<>();
        for (Module m : moduleManager.getModules())
            if (m.isEnabled()) active.add(m);
        if (active.isEmpty()) return;

        active.sort(Comparator.comparingInt(m -> -font.getStringWidth(m.getName())));

        setupOrtho(width, height);
        int y = 4;
        long now = System.currentTimeMillis();
        for (int i = 0; i < active.size(); i++) {
            Module m   = active.get(i);
            float  hue = ((now / 20f + i * 30f) % 360f) / 360f;
            Color  accent = Color.getHSBColor(hue, 0.7f, 1.0f);
            String name = m.getName();
            int    tw   = font.getStringWidth(name);
            int    th   = font.getStringHeight();
            int    x    = width - tw - 10;
            drawRect(x - 4, y - 1, width, y + th + 1, new Color(0, 0, 0, 130));
            drawRect(x - 4, y - 1, x - 2, y + th + 1, accent);
            glEnable(GL_TEXTURE_2D);
            font.drawString(name, x, y, Color.WHITE, true);
            glDisable(GL_TEXTURE_2D);
            y += th + 3;
        }
        restoreOrtho();
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
}
