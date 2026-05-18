package client.client.modules;

import client.Minecraft;
import client.client.module.Module;
import client.net.SocketClient;
import global.Packets;

import java.awt.Color;
import static org.lwjgl.opengl.GL11.*;

public class ScaffoldModule extends Module {

    public static final int[]    BLOCK_IDS   = { 1, 2, 3, 4, 5, 6 };
    public static final String[] BLOCK_NAMES = { "Grass", "Cobblestone", "Dirt", "Obsidian", "Sand", "Bricks" };
    public static final Color[]  BLOCK_COLORS = {
        new Color(86, 130, 54),
        new Color(100, 100, 100),
        new Color(134, 96, 67),
        new Color(30, 20, 40),
        new Color(220, 210, 150),
        new Color(180, 100, 80),
    };

    public static int selectedBlockIndex = 1; // default Cobblestone
    public static boolean submenuOpen = false;

    public ScaffoldModule() {
        super("Scaffold", "Bridges under you as you walk", Category.PLAYER);
        setKeybind(org.lwjgl.input.Keyboard.KEY_X);
    }

    @Override
    public void onDisable() { submenuOpen = false; }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.mc;
        if (mc == null || mc.localPlayer == null || mc.level == null) return;
        if (mc.chat != null && mc.chat.toggled) return;
        if (submenuOpen) return;

        int px = (int) Math.floor(mc.localPlayer.x);
        int pz = (int) Math.floor(mc.localPlayer.z);

        // Place bridge 1 block ahead in movement direction + current pos
        float yaw = ((mc.localPlayer.yRotation % 360) + 360) % 360;
        int dirX = 0, dirZ = 0;
        if      (yaw < 45  || yaw >= 315) { dirZ =  1; }
        else if (yaw < 135)               { dirX = -1; }
        else if (yaw < 225)               { dirZ = -1; }
        else                              { dirX =  1; }

        int blockId = BLOCK_IDS[selectedBlockIndex];
        int baseY = (int) Math.floor(mc.localPlayer.y) - 1;

        // Place under current position and 1 block ahead
        tryPlace(mc, px,        baseY, pz,        blockId);
        tryPlace(mc, px + dirX, baseY, pz + dirZ, blockId);
    }

    private void tryPlace(Minecraft mc, int x, int y, int z, int id) {
        if (!mc.level.isSolidTile(x, y, z)) {
            try {
                mc.level.setTile(x, y, z, id);
                SocketClient.sendBlock(Packets.BLOCK_PLACE, x, y, z, id);
            } catch (Exception e) { }
        }
    }

    @Override
    public void onRender(int width, int height) {
        if (!submenuOpen) return;
        renderBlockPicker(width, height);
    }

    private void renderBlockPicker(int sw, int sh) {
        int itemW = 140, itemH = 24, padding = 10;
        int totalH = BLOCK_NAMES.length * itemH + padding * 2 + 22;
        int px = sw / 2 - itemW / 2;
        int py = sh / 2 - totalH / 2;

        setupOrtho(sw, sh);

        drawRect(px - 2, py - 2, px + itemW + 2, py + totalH + 2, new Color(0,0,0,210));
        drawRect(px, py, px + itemW, py + totalH, new Color(18,18,24,245));
        drawRect(px, py, px + itemW, py + 2, new Color(100, 100, 255, 255));

        glEnable(GL_TEXTURE_2D);
        Minecraft.mc.getClientFont().drawString("Scaffold Block", px + 8, py + padding, new Color(180,180,255), true);
        glDisable(GL_TEXTURE_2D);

        for (int i = 0; i < BLOCK_NAMES.length; i++) {
            int ry = py + padding + 20 + i * itemH;
            boolean sel = (i == selectedBlockIndex);
            drawRect(px + 4, ry, px + itemW - 4, ry + itemH - 2,
                    sel ? new Color(55,55,80,245) : new Color(24,24,32,220));
            if (sel) drawRect(px + 4, ry, px + 6, ry + itemH - 2, new Color(100,100,255,255));

            drawRect(px + 10, ry + 5, px + 22, ry + itemH - 7, BLOCK_COLORS[i]);

            glEnable(GL_TEXTURE_2D);
            Minecraft.mc.getClientFont().drawString(BLOCK_NAMES[i], px + 28,
                    ry + (itemH - Minecraft.mc.getClientFont().getStringHeight()) / 2,
                    sel ? Color.WHITE : new Color(160,160,170), true);
            glDisable(GL_TEXTURE_2D);

            int mx = org.lwjgl.input.Mouse.getX();
            int my = sh - org.lwjgl.input.Mouse.getY() - 1;
            if (org.lwjgl.input.Mouse.isButtonDown(0)
                    && mx >= px+4 && mx < px+itemW-4 && my >= ry && my < ry+itemH-2) {
                selectedBlockIndex = i;
                submenuOpen = false;
            }
        }

        glEnable(GL_TEXTURE_2D);
        Minecraft.mc.getClientFont().drawString("Click to select  |  ESC to close",
                px + 6, py + totalH - 12, new Color(90,90,110), true);
        glDisable(GL_TEXTURE_2D);

        if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_ESCAPE)) {
            submenuOpen = false;
        }

        restoreOrtho();
    }

    public static void openSubmenu() { submenuOpen = true; }

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
