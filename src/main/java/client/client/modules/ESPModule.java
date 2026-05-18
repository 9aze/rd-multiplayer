package client.client.modules;

import client.player.remote.PlayerManager;
import client.player.remote.RemotePlayer;
import client.client.module.Module;

import java.util.Map;

import static org.lwjgl.opengl.GL11.*;

public class ESPModule extends Module {
    public ESPModule() {
        super("ESP", "See players through walls", Category.RENDER);
        setKeybind(org.lwjgl.input.Keyboard.KEY_Z);
    }

    public static void renderESP(PlayerManager pm) {
        if (pm == null) return;
        Map<String, RemotePlayer> players = pm.getPlayers();
        if (players == null || players.isEmpty()) return;

        glPushMatrix();
        glDisable(GL_TEXTURE_2D);
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glLineWidth(2.0f);

        for (RemotePlayer rp : players.values()) {
            double x = rp.x, y = rp.y, z = rp.z;
            float w = 0.35f, h = 1.8f;

            glColor4f(0f, 1f, 1f, 0.6f);
            glBegin(GL_LINE_STRIP);
            glVertex3d(x-w,y-h,z-w); glVertex3d(x+w,y-h,z-w);
            glVertex3d(x+w,y-h,z+w); glVertex3d(x-w,y-h,z+w);
            glVertex3d(x-w,y-h,z-w);
            glEnd();
            glBegin(GL_LINE_STRIP);
            glVertex3d(x-w,y,z-w); glVertex3d(x+w,y,z-w);
            glVertex3d(x+w,y,z+w); glVertex3d(x-w,y,z+w);
            glVertex3d(x-w,y,z-w);
            glEnd();
            glBegin(GL_LINES);
            glVertex3d(x-w,y-h,z-w); glVertex3d(x-w,y,z-w);
            glVertex3d(x+w,y-h,z-w); glVertex3d(x+w,y,z-w);
            glVertex3d(x+w,y-h,z+w); glVertex3d(x+w,y,z+w);
            glVertex3d(x-w,y-h,z+w); glVertex3d(x-w,y,z+w);
            glEnd();
        }

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
        glPopMatrix();
    }
}
