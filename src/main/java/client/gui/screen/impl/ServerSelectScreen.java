package client.gui.screen.impl;

import static org.lwjgl.opengl.GL11.*;
import client.FontRenderer;
import client.Minecraft;
import client.Textures;
import java.awt.*;
import java.util.prefs.Preferences;
import client.gui.screen.Screen;
import client.gui.screen.components.ButtonComponent;
import client.gui.screen.components.FieldComponent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class ServerSelectScreen extends Screen {
    private int bg = -1;
    private FieldComponent fUsername, fServer, fPort;
    private final Preferences prefs = Preferences.userNodeForPackage(ServerSelectScreen.class);
    private ButtonComponent btnConnect, btnBack;
    private long lastBlink = System.currentTimeMillis();
    private boolean cursorVisible = true;
    private boolean prefsLoaded = false;

    @Override
    public void init() {
        super.init();
    }

    @Override
    public void render(FontRenderer font, int width, int height) {
        if (bg == -1) {
            bg = Textures.loadTexture("/client/textures/background.png", GL_NEAREST);
        }

        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, width, height, 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_TEXTURE_2D);

        glColor4f(1f, 1f, 1f, 1f);
        Textures.bind(bg);
        glBegin(GL_QUADS);
        glTexCoord2f(0, 0); glVertex2f(0, 0);
        glTexCoord2f(1, 0); glVertex2f(width, 0);
        glTexCoord2f(1, 1); glVertex2f(width, height);
        glTexCoord2f(0, 1); glVertex2f(0, height);
        glEnd();

        String title = "Connect to Server";
        int titleX = (width - font.getStringWidth(title) * 2) / 2;
        int titleY = height / 6 - font.getStringHeight();

        glDisable(GL_TEXTURE_2D);
        glPushMatrix();
        glTranslatef(titleX, titleY, 0);
        glScalef(2f, 2f, 1f);
        glEnable(GL_TEXTURE_2D);
        font.drawString(title, 0, 0, Color.WHITE, true);
        glDisable(GL_TEXTURE_2D);
        glPopMatrix();
        glEnable(GL_TEXTURE_2D);

        int panelX = (width - Math.min(400, width - 40)) / 2;
        int panelW = Math.min(400, width - 40);
        int serverW = panelW - 80 - 8;
        int row1Y = height / 3;
        int row2Y = row1Y + 14 + 28 + 22;

        if (!prefsLoaded) {
            fUsername = new FieldComponent("USERNAME", panelX, row1Y + 14, panelW, 28);
            fUsername.value = new StringBuilder(prefs.get("username", "Player"));
            fServer = new FieldComponent("SERVER IP", panelX, row2Y + 14, serverW, 28);
            fServer.value = new StringBuilder(prefs.get("ip", "localhost"));
            fPort = new FieldComponent("PORT", panelX + serverW + 8, row2Y + 14, 80, 28);
            fPort.value = new StringBuilder(prefs.get("port", "9090"));
            prefsLoaded = true;
        } else {
            fUsername.x = panelX; fUsername.y = row1Y + 14; fUsername.w = panelW; fUsername.h = 28;
            fServer.x = panelX; fServer.y = row2Y + 14; fServer.w = serverW; fServer.h = 28;
            fPort.x = panelX + serverW + 8; fPort.y = row2Y + 14; fPort.w = 80; fPort.h = 28;
        }

        int btnY = row2Y + 14 + 28 + 28;
        btnConnect = new ButtonComponent("Connect", (width / 2) - 120 - 5, btnY, 120, 28);
        btnBack = new ButtonComponent("Back", (width / 2) + 5, btnY, 120, 28);

        long now = System.currentTimeMillis();
        if (now - lastBlink > 530) { cursorVisible = !cursorVisible; lastBlink = now; }

        int mx = Mouse.getX();
        int my = height - Mouse.getY() - 1;

        while (Mouse.next()) {
            if (Mouse.getEventButton() == 0 && Mouse.getEventButtonState()) {
                fUsername.focused = fUsername.contains(mx, my);
                fServer.focused = fServer.contains(mx, my);
                fPort.focused = fPort.contains(mx, my);
                if (btnConnect.contains(mx, my)) onConnect();
                if (btnBack.contains(mx, my)) onBack();
            }
        }

        FieldComponent focused = fUsername.focused ? fUsername : fServer.focused ? fServer : fPort.focused ? fPort : null;

        while (Keyboard.next()) {
            if (Keyboard.getEventKeyState() && focused != null) {
                int key = Keyboard.getEventKey();
                char ch = Keyboard.getEventCharacter();
                if (key == Keyboard.KEY_BACK) {
                    if (focused.value.length() > 0)
                        focused.value.deleteCharAt(focused.value.length() - 1);
                } else if (key == Keyboard.KEY_TAB) {
                    fUsername.focused = false; fServer.focused = false; fPort.focused = false;
                    if (focused == fUsername) fServer.focused = true;
                    else if (focused == fServer) fPort.focused = true;
                    else fUsername.focused = true;
                    focused = null;
                } else if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
                    onConnect();
                } else if (ch >= 32 && ch != 127) {
                    if (focused == fPort) {
                        if (Character.isDigit(ch) && focused.value.length() < 5)
                            focused.value.append(ch);
                    } else {
                        focused.value.append(ch);
                    }
                }
            }
        }

        drawField(font, fUsername, mx, my, cursorVisible);
        drawField(font, fServer, mx, my, cursorVisible);
        drawField(font, fPort, mx, my, cursorVisible);

        glDisable(GL_TEXTURE_2D);
        for (ButtonComponent btn : new ButtonComponent[]{btnConnect, btnBack}) {
            boolean hov = btn.contains(mx, my);
            glColor4f(hov ? 0.6f : 0.2f, hov ? 0.6f : 0.2f, hov ? 0.6f : 0.2f, 0.85f);
            glBegin(GL_QUADS);
            glVertex2f(btn.x, btn.y); glVertex2f(btn.x + btn.w, btn.y);
            glVertex2f(btn.x + btn.w, btn.y + btn.h); glVertex2f(btn.x, btn.y + btn.h);
            glEnd();
            glColor4f(0.8f, 0.8f, 0.8f, 0.9f);
            glBegin(GL_LINE_LOOP);
            glVertex2f(btn.x, btn.y); glVertex2f(btn.x + btn.w, btn.y);
            glVertex2f(btn.x + btn.w, btn.y + btn.h); glVertex2f(btn.x, btn.y + btn.h);
            glEnd();
            glEnable(GL_TEXTURE_2D);
            font.drawString(btn.label,
                    btn.x + (btn.w - font.getStringWidth(btn.label)) / 2,
                    btn.y + (btn.h - font.getStringHeight()) / 2,
                    hov ? Color.YELLOW : Color.WHITE, true);
            glDisable(GL_TEXTURE_2D);
        }

        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
    }

    private void drawField(FontRenderer font, FieldComponent f, int mx, int my, boolean cursor) {
        boolean hov = f.contains(mx, my);
        glDisable(GL_TEXTURE_2D);
        glColor4f(0.1f, 0.1f, 0.1f, f.focused ? 0.9f : 0.7f);
        glBegin(GL_QUADS);
        glVertex2f(f.x, f.y); glVertex2f(f.x + f.w, f.y);
        glVertex2f(f.x + f.w, f.y + f.h); glVertex2f(f.x, f.y + f.h);
        glEnd();
        if (f.focused) glColor4f(1f, 1f, 1f, 1f);
        else if (hov) glColor4f(0.7f, 0.7f, 0.7f, 1f);
        else glColor4f(0.4f, 0.4f, 0.4f, 1f);
        glBegin(GL_LINE_LOOP);
        glVertex2f(f.x, f.y); glVertex2f(f.x + f.w, f.y);
        glVertex2f(f.x + f.w, f.y + f.h); glVertex2f(f.x, f.y + f.h);
        glEnd();
        glEnable(GL_TEXTURE_2D);
        int lh = font.getStringHeight();
        font.drawString(f.label, f.x, f.y - lh - 2, Color.LIGHT_GRAY, true);
        font.drawString(f.value.toString() + (f.focused && cursor ? "|" : ""), f.x + 6, f.y + (f.h - lh) / 2, Color.WHITE, true);
        glDisable(GL_TEXTURE_2D);
    }

    private void onConnect() {
        String host = fServer.value.toString().trim();
        String user = fUsername.value.toString().trim();
        String portStr = fPort.value.toString().trim();
        prefs.put("ip", host);
        prefs.put("port", portStr);
        prefs.put("username", user);
        if (host.isEmpty() || user.isEmpty()) return;
        int port = 9090;
        try { port = Integer.parseInt(portStr); } catch (NumberFormatException ignored) {}
        Minecraft.mc.connect(host, port, user);
    }

    private void onBack() {
        Minecraft.mc.setScreen(new MenuScreen());
    }

    @Override
    public void destroy() {
        super.destroy();
    }
}