package client.gui.screen.impl;

import static org.lwjgl.opengl.GL11.*;

import client.FontRenderer;
import client.Minecraft;
import client.Textures;
import java.awt.*;

import client.gui.screen.Screen;
import client.gui.screen.components.ButtonComponent;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

public class MenuScreen extends Screen {
    public String titleText = "rd-multiplayer";
    public String versionText = "v" + Minecraft.GIT_HASH;

    int bg = -1;

    private ButtonComponent[] buttons;

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
        glTexCoord2f(0, 0);
        glVertex2f(0, 0);
        glTexCoord2f(1, 0);
        glVertex2f(width, 0);
        glTexCoord2f(1, 1);
        glVertex2f(width, height);
        glTexCoord2f(0, 1);
        glVertex2f(0, height);
        glEnd();

        int titleW = font.getStringWidth(titleText) * 2;
        int titleH = font.getStringHeight() * 2;
        int titleX = (width - titleW) / 2;
        int titleY = height / 4 - titleH / 2;

        glDisable(GL_TEXTURE_2D);
        glPushMatrix();
        glTranslatef(titleX, titleY, 0);
        glScalef(2f, 2f, 1f);
        glEnable(GL_TEXTURE_2D);
        font.drawString(titleText, 0, 0, Color.WHITE, true);
        glDisable(GL_TEXTURE_2D);
        glPopMatrix();
        glEnable(GL_TEXTURE_2D);

        int verW = font.getStringWidth(versionText);
        int verX = (width - verW) / 2;
        int verY = titleY + titleH + 6;
        font.drawString(versionText, verX, verY, Color.LIGHT_GRAY, true);

        int btnW = 160;
        int btnH = 28;
        int btnGap = 10;
        int totalBtnH = 3 * btnH + 2 * btnGap;
        int startY = height / 2 + height / 8 - totalBtnH / 2;
        int btnX = (width - btnW) / 2;

        buttons = new ButtonComponent[] {
                new ButtonComponent("Play", btnX, startY, btnW, btnH),
                new ButtonComponent("Options", btnX, startY + btnH + btnGap, btnW, btnH),
                new ButtonComponent("Quit", btnX, startY + (btnH + btnGap) * 2, btnW, btnH),
        };

        int mx = Mouse.getX();
        int myFlipped = height - Mouse.getY() - 1;

        while (Mouse.next()) {
            if (Mouse.getEventButton() == 0 && Mouse.getEventButtonState()) {
                for (ButtonComponent btn : buttons) {
                    if (btn.contains(mx, myFlipped)) {
                        onButtonClicked(btn.label);
                    }
                }
            }
        }

        glDisable(GL_TEXTURE_2D);
        for (ButtonComponent btn : buttons) {
            boolean hovered = btn.contains(mx, myFlipped);

            if (hovered) {
                glColor4f(0.6f, 0.6f, 0.6f, 0.85f);
            } else {
                glColor4f(0.2f, 0.2f, 0.2f, 0.75f);
            }
            glBegin(GL_QUADS);
            glVertex2f(btn.x, btn.y);
            glVertex2f(btn.x + btn.w, btn.y);
            glVertex2f(btn.x + btn.w, btn.y + btn.h);
            glVertex2f(btn.x, btn.y + btn.h);
            glEnd();

            glColor4f(0.8f, 0.8f, 0.8f, 0.9f);
            glBegin(GL_LINE_LOOP);
            glVertex2f(btn.x, btn.y);
            glVertex2f(btn.x + btn.w, btn.y);
            glVertex2f(btn.x + btn.w, btn.y + btn.h);
            glVertex2f(btn.x, btn.y + btn.h);
            glEnd();

            glEnable(GL_TEXTURE_2D);
            int lw = font.getStringWidth(btn.label);
            int lh = font.getStringHeight();
            int lx = btn.x + (btn.w - lw) / 2;
            int ly = btn.y + (btn.h - lh) / 2;
            Color labelColor = hovered ? Color.YELLOW : Color.WHITE;
            font.drawString(btn.label, lx, ly, labelColor, true);
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

    private void onButtonClicked(String label) {
        if(label.equals("Play")) {
            Minecraft.mc.setScreen(new ServerSelectScreen());
        }
        if (label.equals("Quit")) {
            Display.destroy();
            System.exit(0);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
    }
}