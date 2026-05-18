package client.client.modules;

import client.Minecraft;
import client.client.module.Module;
import client.player.remote.RemotePlayer;

import java.awt.Color;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;

public class ChamsModule extends Module {

    public ChamsModule() {
        super("Chams", "Render players as solid glowing colors through walls", Category.RENDER);
        setKeybind(org.lwjgl.input.Keyboard.KEY_C);
    }

    public static void renderChams(Minecraft mc) {
        if (mc == null || mc.playerManager == null) return;

        Map<String, RemotePlayer> players = mc.playerManager.getPlayers();
        if (players == null || players.isEmpty()) return;

        glPushMatrix();
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        long now = System.currentTimeMillis();
        int i = 0;

        for (RemotePlayer rp : players.values()) {
            // Cycle color per player
            float hue = ((now / 40f + i * 60f) % 360f) / 360f;
            Color c = Color.getHSBColor(hue, 1.0f, 1.0f);

            // Pass 1: through walls (depth test off) — transparent
            glDisable(GL_DEPTH_TEST);
            glColor4f(c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f, 0.35f);
            renderPlayerBox(rp.x, rp.y, rp.z);

            // Pass 2: visible normally — solid
            glEnable(GL_DEPTH_TEST);
            glColor4f(c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f, 0.75f);
            renderPlayerBox(rp.x, rp.y, rp.z);

            i++;
        }

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
        glPopMatrix();
    }

    private static void renderPlayerBox(double x, double y, double z) {
        double w = 0.35;
        double h = 1.8;
        double top = y;
        double bot = y - h;

        // Front
        glBegin(GL_QUADS);
        glVertex3d(x-w, bot, z+w); glVertex3d(x+w, bot, z+w);
        glVertex3d(x+w, top, z+w); glVertex3d(x-w, top, z+w);
        glEnd();
        // Back
        glBegin(GL_QUADS);
        glVertex3d(x+w, bot, z-w); glVertex3d(x-w, bot, z-w);
        glVertex3d(x-w, top, z-w); glVertex3d(x+w, top, z-w);
        glEnd();
        // Left
        glBegin(GL_QUADS);
        glVertex3d(x-w, bot, z-w); glVertex3d(x-w, bot, z+w);
        glVertex3d(x-w, top, z+w); glVertex3d(x-w, top, z-w);
        glEnd();
        // Right
        glBegin(GL_QUADS);
        glVertex3d(x+w, bot, z+w); glVertex3d(x+w, bot, z-w);
        glVertex3d(x+w, top, z-w); glVertex3d(x+w, top, z+w);
        glEnd();
        // Top
        glBegin(GL_QUADS);
        glVertex3d(x-w, top, z-w); glVertex3d(x+w, top, z-w);
        glVertex3d(x+w, top, z+w); glVertex3d(x-w, top, z+w);
        glEnd();
        // Bottom
        glBegin(GL_QUADS);
        glVertex3d(x-w, bot, z+w); glVertex3d(x+w, bot, z+w);
        glVertex3d(x+w, bot, z-w); glVertex3d(x-w, bot, z-w);
        glEnd();
    }
}
