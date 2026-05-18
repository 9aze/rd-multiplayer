package client.client.modules;

import client.Minecraft;
import client.client.module.Module;
import client.level.Level;
import client.player.remote.RemotePlayer;

import java.awt.Color;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;

public class RadarModule extends Module {

    private static final int RADAR_SIZE = 120; // total pixel diameter
    private static final int RANGE      = 32;  // world blocks shown in each direction
    private static final int PADDING    = 14;  // px from top-right corner

    public RadarModule() {
        super("Radar", "Minimap with terrain and players", Category.RENDER);
        setKeybind(org.lwjgl.input.Keyboard.KEY_M);
    }

    public static void renderRadar(int sw, int sh) {
        Minecraft mc = Minecraft.mc;
        if (mc == null || mc.localPlayer == null) return;

        int r  = RADAR_SIZE / 2;
        // Centre of radar circle — fully inside screen
        int cx = sw - r - PADDING;
        int cy = r + PADDING;

        setupOrtho(sw, sh);

        // ── Background ──────────────────────────────────────────────────────
        glColor4f(0f, 0f, 0f, 0.75f);
        fillCircle(cx, cy, r);

        // ── Terrain ─────────────────────────────────────────────────────────
        Level level = mc.level;
        if (level != null) {
            int px = (int) mc.localPlayer.x;
            int pz = (int) mc.localPlayer.z;
            int playerY = (int) mc.localPlayer.y;

            // One pixel = (RADAR_SIZE / (RANGE*2)) world blocks
            float scale = (float) r / RANGE;

            for (int dx = -RANGE; dx <= RANGE; dx++) {
                for (int dz = -RANGE; dz <= RANGE; dz++) {
                    // Only draw inside the circle
                    if (dx * dx + dz * dz > RANGE * RANGE) continue;

                    int wx = px + dx;
                    int wz = pz + dz;

                    // Find topmost solid block at this column
                    byte blockId = 0;
                    for (int wy = playerY + 4; wy >= 0; wy--) {
                        byte b = level.getRawBlock(wx, wy, wz);
                        if (b != 0) { blockId = b; break; }
                    }

                    if (blockId == 0) continue;

                    Color c = blockColor(blockId);

                    // Rotate relative to player yaw
                    double yaw   = Math.toRadians(mc.localPlayer.yRotation);
                    double rdx   =  dx * Math.cos(yaw) + dz * Math.sin(yaw);
                    double rdz   = -dx * Math.sin(yaw) + dz * Math.cos(yaw);

                    int sx = cx + (int)(rdx * scale);
                    int sy = cy + (int)(rdz * scale);

                    int ps = Math.max(1, (int) scale); // pixel size

                    glColor4f(c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f, 0.9f);
                    glBegin(GL_QUADS);
                    glVertex2f(sx,    sy);
                    glVertex2f(sx+ps, sy);
                    glVertex2f(sx+ps, sy+ps);
                    glVertex2f(sx,    sy+ps);
                    glEnd();
                }
            }
        }

        // ── Border ──────────────────────────────────────────────────────────
        glColor4f(0.5f, 0.5f, 0.5f, 1f);
        glLineWidth(1.5f);
        strokeCircle(cx, cy, r);

        // ── Crosshairs ──────────────────────────────────────────────────────
        glColor4f(0.3f, 0.3f, 0.3f, 0.6f);
        glBegin(GL_LINES);
        glVertex2f(cx - r, cy); glVertex2f(cx + r, cy);
        glVertex2f(cx, cy - r); glVertex2f(cx, cy + r);
        glEnd();

        // ── Players ─────────────────────────────────────────────────────────
        if (mc.playerManager != null) {
            Map<String, RemotePlayer> players = mc.playerManager.getPlayers();
            long now = System.currentTimeMillis();
            int i = 0;
            for (RemotePlayer rp : players.values()) {
                double ddx = rp.x - mc.localPlayer.x;
                double ddz = rp.z - mc.localPlayer.z;

                double yaw  = Math.toRadians(mc.localPlayer.yRotation);
                double rdx  =  ddx * Math.cos(yaw) + ddz * Math.sin(yaw);
                double rdz  = -ddx * Math.sin(yaw) + ddz * Math.cos(yaw);

                float scale = (float) r / RANGE;
                float dotX  = cx + (float)(rdx * scale);
                float dotZ  = cy + (float)(rdz * scale);

                // Clamp to edge
                float fx = dotX - cx, fz = dotZ - cy;
                float dist = (float) Math.sqrt(fx*fx + fz*fz);
                if (dist > r - 5) {
                    dotX = cx + fx / dist * (r - 5);
                    dotZ = cy + fz / dist * (r - 5);
                }

                float hue = ((now / 40f + i * 60f) % 360f) / 360f;
                Color c = Color.getHSBColor(hue, 0.9f, 1.0f);
                glColor4f(c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f, 1f);
                dot(dotX, dotZ, 4);
                i++;
            }
        }

        // ── Self (white triangle pointing forward) ───────────────────────────
        glColor4f(1f, 1f, 1f, 1f);
        dot(cx, cy, 5);

        restoreOrtho();
    }

    private static Color blockColor(byte id) {
        switch (id & 0xFF) {
            case 1:  return new Color(128, 128, 128); // stone
            case 2:  return new Color(86,  130, 54);  // grass
            case 3:  return new Color(134, 96,  67);  // dirt
            case 4:  return new Color(100, 100, 100); // cobblestone
            case 5:  return new Color(157, 128, 79);  // wood planks
            case 8:
            case 9:  return new Color(20,  80,  200); // water
            case 10:
            case 11: return new Color(220, 80,  20);  // lava
            case 12: return new Color(220, 210, 150); // sand
            case 13: return new Color(120, 110, 100); // gravel
            case 17: return new Color(100, 75,  40);  // log
            case 18: return new Color(60,  100, 40);  // leaves
            case 20: return new Color(150, 200, 220); // glass
            default: return new Color(160, 160, 160); // unknown solid
        }
    }

    private static void dot(float x, float y, int size) {
        glBegin(GL_QUADS);
        glVertex2f(x - size/2f, y - size/2f);
        glVertex2f(x + size/2f, y - size/2f);
        glVertex2f(x + size/2f, y + size/2f);
        glVertex2f(x - size/2f, y + size/2f);
        glEnd();
    }

    private static void fillCircle(int cx, int cy, int r) {
        glBegin(GL_TRIANGLE_FAN);
        glVertex2f(cx, cy);
        for (int i = 0; i <= 64; i++) {
            double a = 2 * Math.PI * i / 64;
            glVertex2f((float)(cx + r * Math.cos(a)), (float)(cy + r * Math.sin(a)));
        }
        glEnd();
    }

    private static void strokeCircle(int cx, int cy, int r) {
        glBegin(GL_LINE_LOOP);
        for (int i = 0; i < 64; i++) {
            double a = 2 * Math.PI * i / 64;
            glVertex2f((float)(cx + r * Math.cos(a)), (float)(cy + r * Math.sin(a)));
        }
        glEnd();
    }

    private static void setupOrtho(int w, int h) {
        glMatrixMode(GL_PROJECTION); glPushMatrix(); glLoadIdentity();
        glOrtho(0, w, h, 0, -1, 1);
        glMatrixMode(GL_MODELVIEW); glPushMatrix(); glLoadIdentity();
        glDisable(GL_DEPTH_TEST); glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND); glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_TEXTURE_2D);
    }

    private static void restoreOrtho() {
        glEnable(GL_DEPTH_TEST); glEnable(GL_CULL_FACE); glDisable(GL_BLEND);
        glPopMatrix(); glMatrixMode(GL_PROJECTION); glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
    }
}
