package client.client.modules;

import client.Minecraft;
import client.client.module.Module;
import client.net.SocketClient;
import global.Packets;

import java.awt.Color;
import static org.lwjgl.opengl.GL11.*;

public class SpamPlaceModule extends Module {

    public static final int[]    BLOCK_IDS    = { 1, 2, 3, 4, 5, 6 };
    public static final String[] BLOCK_NAMES  = { "Grass", "Cobblestone", "Dirt", "Obsidian", "Sand", "Bricks" };
    public static final Color[]  BLOCK_COLORS = {
        new Color(86,130,54), new Color(100,100,100), new Color(134,96,67),
        new Color(30,20,40),  new Color(220,210,150), new Color(180,100,80),
    };

    public static int  selectedBlockIndex = 1;
    public static boolean submenuOpen = false;

    private static final int    RADIUS       = 6;
    private static final double EYE_HEIGHT   = 1.6;
    private static final double MAX_REACH    = 9.0;
    private static final long   MS_PER_PLACE = 220;
    private static final int    PER_TICK     = 4; // place 4 blocks at once
    private long lastPlace = 0;

    public SpamPlaceModule() {
        super("SpamPlace", "Rapidly fills area with blocks", Category.MISC);
        setKeybind(org.lwjgl.input.Keyboard.KEY_INSERT);
    }

    @Override public void onDisable() { submenuOpen = false; }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.mc;
        if (mc == null || mc.localPlayer == null || mc.level == null) return;
        if (mc.chat != null && mc.chat.toggled) return;
        if (submenuOpen) return;

        long now = System.currentTimeMillis();
        if (now - lastPlace < MS_PER_PLACE) return;
        lastPlace = now;

        int cx = (int) Math.floor(mc.localPlayer.x);
        int cy = (int) Math.floor(mc.localPlayer.y);
        int cz = (int) Math.floor(mc.localPlayer.z);
        int blockId = BLOCK_IDS[selectedBlockIndex];
        int placed = 0;

        outer:
        for (int dy = -1; dy <= 3; dy++) {
            for (int dx = -RADIUS; dx <= RADIUS; dx++) {
                for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                    if (placed >= PER_TICK) break outer;
                    int bx = cx+dx, by = cy+dy, bz = cz+dz;
                    if (mc.level.isSolidTile(bx, by, bz)) continue;
                    if (Math.abs(dx)<=1 && dy>=-1 && dy<=1 && Math.abs(dz)<=1) continue;

                    double ddx = (bx+0.5)-mc.localPlayer.x;
                    double ddy = (by+0.5)-(mc.localPlayer.y+EYE_HEIGHT);
                    double ddz = (bz+0.5)-mc.localPlayer.z;
                    if (Math.sqrt(ddx*ddx+ddy*ddy+ddz*ddz) > MAX_REACH) continue;

                    try {
                        mc.level.setTile(bx, by, bz, blockId);
                        SocketClient.sendBlock(Packets.BLOCK_PLACE, bx, by, bz, blockId);
                        placed++;
                    } catch (Exception e) { }
                }
            }
        }
    }

    @Override
    public void onRender(int width, int height) {
        if (!submenuOpen) return;
        renderPicker(width, height);
    }

    private void renderPicker(int sw, int sh) {
        int iw = 150, ih = 24, pad = 12;
        int totalH = BLOCK_NAMES.length * ih + pad*2 + 22;
        int px = sw/2 - iw/2, py = sh/2 - totalH/2;

        setupOrtho(sw, sh);
        drawRect(px-2,py-2,px+iw+2,py+totalH+2, new Color(0,0,0,210));
        drawRect(px,py,px+iw,py+totalH, new Color(18,18,24,245));
        drawRect(px,py,px+iw,py+2, new Color(100,200,255,255));

        glEnable(GL_TEXTURE_2D);
        Minecraft.mc.getClientFont().drawString("SpamPlace Block", px+8, py+8, new Color(100,200,255), true);
        glDisable(GL_TEXTURE_2D);

        for (int i = 0; i < BLOCK_NAMES.length; i++) {
            int ry = py+pad+20+i*ih;
            boolean sel = i==selectedBlockIndex;
            drawRect(px+4,ry,px+iw-4,ry+ih-2, sel?new Color(50,50,70,245):new Color(24,24,32,220));
            if (sel) drawRect(px+4,ry,px+6,ry+ih-2, new Color(100,200,255,255));
            drawRect(px+10,ry+5,px+22,ry+ih-7, BLOCK_COLORS[i]);
            glEnable(GL_TEXTURE_2D);
            Minecraft.mc.getClientFont().drawString(BLOCK_NAMES[i], px+28,
                ry+(ih-Minecraft.mc.getClientFont().getStringHeight())/2,
                sel?Color.WHITE:new Color(160,160,170), true);
            glDisable(GL_TEXTURE_2D);

            int mx=org.lwjgl.input.Mouse.getX(), my=sh-org.lwjgl.input.Mouse.getY()-1;
            if (org.lwjgl.input.Mouse.isButtonDown(0) && mx>=px+4&&mx<px+iw-4&&my>=ry&&my<ry+ih-2) {
                selectedBlockIndex=i; submenuOpen=false;
            }
        }
        glEnable(GL_TEXTURE_2D);
        Minecraft.mc.getClientFont().drawString("Click to select  |  ESC to close",
            px+6, py+totalH-12, new Color(90,90,110), true);
        glDisable(GL_TEXTURE_2D);

        if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_ESCAPE)) submenuOpen=false;
        restoreOrtho();
    }

    public static void openSubmenu() { submenuOpen = true; }

    private void setupOrtho(int w,int h){glMatrixMode(GL_PROJECTION);glPushMatrix();glLoadIdentity();glOrtho(0,w,h,0,-1,1);glMatrixMode(GL_MODELVIEW);glPushMatrix();glLoadIdentity();glDisable(GL_DEPTH_TEST);glDisable(GL_CULL_FACE);glEnable(GL_BLEND);glBlendFunc(GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA);glDisable(GL_TEXTURE_2D);}
    private void restoreOrtho(){glEnable(GL_DEPTH_TEST);glEnable(GL_CULL_FACE);glDisable(GL_BLEND);glPopMatrix();glMatrixMode(GL_PROJECTION);glPopMatrix();glMatrixMode(GL_MODELVIEW);}
    private void drawRect(int x1,int y1,int x2,int y2,Color c){glColor4f(c.getRed()/255f,c.getGreen()/255f,c.getBlue()/255f,c.getAlpha()/255f);glBegin(GL_QUADS);glVertex2f(x1,y1);glVertex2f(x2,y1);glVertex2f(x2,y2);glVertex2f(x1,y2);glEnd();}
}
