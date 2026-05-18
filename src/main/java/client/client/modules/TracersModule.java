package client.client.modules;

import client.Minecraft;
import client.client.module.Module;
import client.player.remote.RemotePlayer;

import java.awt.Color;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;

public class TracersModule extends Module {
    public TracersModule() {
        super("Tracers", "Draw lines to all players", Category.RENDER);
        setKeybind(org.lwjgl.input.Keyboard.KEY_T);
    }

    public static void renderTracers(int sw, int sh) {
        Minecraft mc = Minecraft.mc;
        if (mc == null || mc.playerManager == null || mc.localPlayer == null) return;

        Map<String, RemotePlayer> players = mc.playerManager.getPlayers();
        if (players == null || players.isEmpty()) return;

        // Setup ortho for 2D lines from screen center to projected player positions
        // We'll draw in 3D space before ortho - simple line from camera origin to player
        glPushMatrix();
        glDisable(GL_TEXTURE_2D);
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glLineWidth(1.5f);

        long now = System.currentTimeMillis();
        int i = 0;
        for (RemotePlayer rp : players.values()) {
            // Rainbow color per player
            float hue = ((now / 30f + i * 45f) % 360f) / 360f;
            Color c = Color.getHSBColor(hue, 0.8f, 1.0f);
            glColor4f(c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f, 0.8f);

            // Line from local player eye to remote player chest
            glBegin(GL_LINES);
            glVertex3d(mc.localPlayer.x, mc.localPlayer.y, mc.localPlayer.z);
            glVertex3d(rp.x, rp.y - 0.9, rp.z);
            glEnd();
            i++;
        }

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
        glPopMatrix();
    }
}
