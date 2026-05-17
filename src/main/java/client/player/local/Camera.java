package client.player.local;

import client.Minecraft;
import org.lwjgl.BufferUtils;

import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.util.glu.GLU.gluPerspective;
import static org.lwjgl.util.glu.GLU.gluPickMatrix;

public class Camera {

    public static final int FIRST = 0;
    public static final int THIRD = 1;
    public static final int SECOND = 2;
    private static final float DISTANCE = 4.0f;

    public int mode = FIRST;

    private final Minecraft mc;
    private final IntBuffer viewportBuffer = BufferUtils.createIntBuffer(16);

    public Camera(Minecraft mc) {
        this.mc = mc;
    }

    public void cycle() {
        mode = (mode + 1) % 3;
    }

    public void setup(float pt) {
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        gluPerspective(70, mc.width / (float) mc.height, 0.05F, 1000);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        moveToPlayer(pt, mode);
    }

    public void setupPick(float pt, int x, int y) {
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        viewportBuffer.clear();
        glGetInteger(GL_VIEWPORT, viewportBuffer);
        viewportBuffer.flip();
        viewportBuffer.limit(16);
        gluPickMatrix(x, y, 5f, 5f, viewportBuffer);
        gluPerspective(70f, mc.width / (float) mc.height, 0.05f, 1000f);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        moveToPlayer(pt, FIRST);
    }

    private void moveToPlayer(float pt, int camMode) {
        LocalPlayer p = mc.localPlayer;

        glTranslatef(0f, 0f, -0.3f);

        if (camMode != FIRST) {
            glTranslatef(0f, 0f, -DISTANCE);
        }

        float pitch = (camMode == SECOND) ? -p.xRotation : p.xRotation;
        glRotatef(pitch, 1f, 0f, 0f);
        glRotatef(p.yRotation, 0f, 1f, 0f);

        if (camMode == SECOND) {
            glRotatef(180f, 0f, 1f, 0f);
        }

        double x = p.prevX + (p.x - p.prevX) * pt;
        double y = p.prevY + (p.y - p.prevY) * pt;
        double z = p.prevZ + (p.z - p.prevZ) * pt;
        glTranslated(-x, -y, -z);
    }
}