package client.client.modules;

import client.Minecraft;
import client.client.module.Module;
import client.net.SocketClient;

import java.awt.Color;
import static org.lwjgl.opengl.GL11.*;

public class AnnoyModule extends Module {

    public static String  message     = "lol get trolled";
    public static boolean submenuOpen = false;
    private static final StringBuilder inputBuf = new StringBuilder("lol get trolled");

    private static long lastSend   = 0;
    private static final long INTERVAL = 300;

    public AnnoyModule() {
        super("Annoy", "Spam a message in chat", Category.MISC);
        setKeybind(org.lwjgl.input.Keyboard.KEY_COMMA);
    }

    @Override public void onDisable() { submenuOpen = false; }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.mc;
        if (mc == null || mc.localPlayer == null) return;
        if (submenuOpen) return;
        long now = System.currentTimeMillis();
        if (now - lastSend < INTERVAL) return;
        lastSend = now;
        try { SocketClient.sendChat(mc.username, message); }
        catch (Exception e) { }
    }

    // Called from ClientMod when submenu is open
    public static void handleKey(int key, char c) {
        if (key == org.lwjgl.input.Keyboard.KEY_BACK) {
            if (inputBuf.length() > 0)
                inputBuf.deleteCharAt(inputBuf.length()-1);
        } else if (key == org.lwjgl.input.Keyboard.KEY_RETURN) {
            message     = inputBuf.toString();
            submenuOpen = false;
        } else if (key == org.lwjgl.input.Keyboard.KEY_ESCAPE) {
            submenuOpen = false;
        } else if (c >= 32 && c < 127 && inputBuf.length() < 60) {
            inputBuf.append(c);
        }
    }

    @Override
    public void onRender(int width, int height) {
        if (!submenuOpen) return;

        int pw = 320, ph = 96;
        int px = width/2 - pw/2, py = height/2 - ph/2;

        setupOrtho(width, height);

        // Shadow
        drawRect(px+4, py+4, px+pw+4, py+ph+4, new Color(0,0,0,70));
        // Body
        drawRect(px, py, px+pw, py+ph, new Color(14,14,18,250));
        // Top accent
        drawRect(px, py, px+pw, py+3, new Color(255,180,40,255));

        glEnable(GL_TEXTURE_2D);
        Minecraft.mc.getClientFont().drawString("Annoy Message", px+10, py+10,
            new Color(255,200,70), true);
        glDisable(GL_TEXTURE_2D);

        // Input box
        drawRect(px+8,  py+30, px+pw-8, py+56, new Color(22,22,30,255));
        drawRect(px+8,  py+30, px+pw-8, py+32, new Color(255,180,40,200));
        drawRect(px+8,  py+54, px+pw-8, py+56, new Color(60,60,80,180));

        // Text + cursor
        String display = inputBuf.toString() + "|";
        glEnable(GL_TEXTURE_2D);
        Minecraft.mc.getClientFont().drawString(display, px+12, py+38, Color.WHITE, true);

        // Hint
        Minecraft.mc.getClientFont().drawString(
            "Type  |  ENTER = save  |  ESC = cancel",
            px+10, py+ph-18, new Color(80,80,100), true);
        glDisable(GL_TEXTURE_2D);

        restoreOrtho();
    }

    public static void openSubmenu() {
        inputBuf.setLength(0);
        inputBuf.append(message);
        submenuOpen = true;
    }

    private void setupOrtho(int w,int h){glMatrixMode(GL_PROJECTION);glPushMatrix();glLoadIdentity();glOrtho(0,w,h,0,-1,1);glMatrixMode(GL_MODELVIEW);glPushMatrix();glLoadIdentity();glDisable(GL_DEPTH_TEST);glDisable(GL_CULL_FACE);glEnable(GL_BLEND);glBlendFunc(GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA);glDisable(GL_TEXTURE_2D);}
    private void restoreOrtho(){glEnable(GL_DEPTH_TEST);glEnable(GL_CULL_FACE);glDisable(GL_BLEND);glPopMatrix();glMatrixMode(GL_PROJECTION);glPopMatrix();glMatrixMode(GL_MODELVIEW);}
    private void drawRect(int x1,int y1,int x2,int y2,Color c){glColor4f(c.getRed()/255f,c.getGreen()/255f,c.getBlue()/255f,c.getAlpha()/255f);glBegin(GL_QUADS);glVertex2f(x1,y1);glVertex2f(x2,y1);glVertex2f(x2,y2);glVertex2f(x1,y2);glEnd();}
}
