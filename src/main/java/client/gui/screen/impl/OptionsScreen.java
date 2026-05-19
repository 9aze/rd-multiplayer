package client.gui.screen.impl;

import static org.lwjgl.opengl.GL11.*;

import client.FontRenderer;
import client.Minecraft;
import client.Settings;
import client.Textures;
import client.gui.screen.Screen;
import client.gui.screen.components.ButtonComponent;
import client.gui.screen.components.SliderComponent;
import client.net.SocketClient;

import org.lwjgl.input.Mouse;

import java.awt.*;

public class OptionsScreen extends Screen {

    private int bg = -1;

    private SliderComponent sGamma, sRenderDist;
    private ButtonComponent btnBack;

    private boolean draggingGamma = false;
    private boolean draggingRender = false;

    private final Screen returnScreen;
    private final boolean backToInGame;

    public OptionsScreen() { this(null, false); }
    public OptionsScreen(Screen returnScreen) { this(returnScreen, false); }
    private OptionsScreen(Screen returnScreen, boolean backToInGame) {
        this.returnScreen = returnScreen;
        this.backToInGame = backToInGame;
    }

    public static OptionsScreen forInGame() {
        return new OptionsScreen(null, true);
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

        // background
        glColor4f(1f, 1f, 1f, 1f);
        Textures.bind(bg);
        glBegin(GL_QUADS);
        glTexCoord2f(0, 0); glVertex2f(0, 0);
        glTexCoord2f(1, 0); glVertex2f(width, 0);
        glTexCoord2f(1, 1); glVertex2f(width, height);
        glTexCoord2f(0, 1); glVertex2f(0, height);
        glEnd();

        // title
        String title = "Options";
        int titleW = font.getStringWidth(title) * 2;
        int titleX = (width - titleW) / 2;
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

        // sliders panel
        int panelW = Math.min(400, width - 40);
        int panelX = (width - panelW) / 2;
        int rowH = 60;
        int row1Y = height / 3 + 20;
        int row2Y = row1Y + rowH;

        if (sGamma == null) {
            sGamma = new SliderComponent("Brightness", panelX, row1Y, panelW, 18, 0f, 1f, Settings.getGamma());
            sGamma.formatter = v -> String.format("%d%%", (int)(v * 100f));
        } else {
            sGamma.x = panelX; sGamma.y = row1Y; sGamma.w = panelW;
        }

        if (sRenderDist == null) {
            sRenderDist = new SliderComponent("Render Distance", panelX, row2Y, panelW, 18, 2f, 32f, Settings.getRenderDistance());
            sRenderDist.formatter = v -> v.intValue() + " chunks";
        } else {
            sRenderDist.x = panelX; sRenderDist.y = row2Y; sRenderDist.w = panelW;
        }

        int backY = row2Y + 56;
        btnBack = new ButtonComponent("Back", (width - 160) / 2, backY, 160, 28);

        int mx = Mouse.getX();
        int my = height - Mouse.getY() - 1;

        // input
        while (Mouse.next()) {
            if (Mouse.getEventButton() == 0) {
                if (Mouse.getEventButtonState()) {
                    // mouse down
                    if (hitTrack(sGamma, mx, my))   { draggingGamma  = true;  sGamma.setFromMouse(mx);   commitGamma(); }
                    if (hitTrack(sRenderDist, mx, my)) { draggingRender = true; sRenderDist.setFromMouse(mx); commitRenderDistance(); }
                    if (btnBack.contains(mx, my)) onBack();
                } else {
                    // mouse up
                    draggingGamma = false;
                    draggingRender = false;
                }
            }
        }

        if (draggingGamma) { sGamma.setFromMouse(mx);   commitGamma(); }
        if (draggingRender) { sRenderDist.setFromMouse(mx); commitRenderDistance(); }

        // render sliders
        drawSlider(font, sGamma, mx, my);
        drawSlider(font, sRenderDist, mx, my);

        // back button
        glDisable(GL_TEXTURE_2D);
        boolean hovBack = btnBack.contains(mx, my);
        glColor4f(hovBack ? 0.6f : 0.2f, hovBack ? 0.6f : 0.2f, hovBack ? 0.6f : 0.2f, 0.85f);
        glBegin(GL_QUADS);
        glVertex2f(btnBack.x, btnBack.y);
        glVertex2f(btnBack.x + btnBack.w, btnBack.y);
        glVertex2f(btnBack.x + btnBack.w, btnBack.y + btnBack.h);
        glVertex2f(btnBack.x, btnBack.y + btnBack.h);
        glEnd();
        glColor4f(0.8f, 0.8f, 0.8f, 0.9f);
        glBegin(GL_LINE_LOOP);
        glVertex2f(btnBack.x, btnBack.y);
        glVertex2f(btnBack.x + btnBack.w, btnBack.y);
        glVertex2f(btnBack.x + btnBack.w, btnBack.y + btnBack.h);
        glVertex2f(btnBack.x, btnBack.y + btnBack.h);
        glEnd();
        glEnable(GL_TEXTURE_2D);
        font.drawString(btnBack.label, btnBack.x + (btnBack.w - font.getStringWidth(btnBack.label)) / 2, btnBack.y + (btnBack.h - font.getStringHeight()) / 2, hovBack ? Color.YELLOW : Color.WHITE, true);

        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
    }

    private boolean hitTrack(SliderComponent s, int mx, int my) {
        int trackY = s.y + s.h / 2 - 2;
        int trackH = 4;
        return mx >= s.x && mx <= s.x + s.w && my >= s.y - 2 && my <= s.y + s.h + 4;
    }

    private void drawSlider(FontRenderer font, SliderComponent s, int mx, int my) {
        int lh = font.getStringHeight();

        // Label above
        glEnable(GL_TEXTURE_2D);
        font.drawString(s.label, s.x, s.y - lh - 4, Color.LIGHT_GRAY, true);

        // Value to the right of the label
        String valStr = (s.formatter != null) ? s.formatter.apply(s.value) : String.format("%.2f", s.value);
        int valW = font.getStringWidth(valStr);
        font.drawString(valStr, s.x + s.w - valW, s.y - lh - 4, Color.WHITE, true);

        // Track
        glDisable(GL_TEXTURE_2D);
        int trackY = s.y + s.h / 2 - 2;
        glColor4f(0.15f, 0.15f, 0.15f, 0.9f);
        glBegin(GL_QUADS);
        glVertex2f(s.x, trackY);
        glVertex2f(s.x + s.w, trackY);
        glVertex2f(s.x + s.w, trackY + 4);
        glVertex2f(s.x, trackY + 4);
        glEnd();
        glColor4f(0.45f, 0.45f, 0.45f, 1f);
        glBegin(GL_LINE_LOOP);
        glVertex2f(s.x, trackY);
        glVertex2f(s.x + s.w, trackY);
        glVertex2f(s.x + s.w, trackY + 4);
        glVertex2f(s.x, trackY + 4);
        glEnd();

        // Knob
        int kx = s.knobX();
        int kw = 8, kh = s.h;
        boolean hovKnob = hitTrack(s, mx, my);
        glColor4f(hovKnob ? 0.9f : 0.7f, hovKnob ? 0.9f : 0.7f, hovKnob ? 0.9f : 0.7f, 1f);
        glBegin(GL_QUADS);
        glVertex2f(kx - kw / 2f, s.y);
        glVertex2f(kx + kw / 2f, s.y);
        glVertex2f(kx + kw / 2f, s.y + kh);
        glVertex2f(kx - kw / 2f, s.y + kh);
        glEnd();
        glColor4f(0.2f, 0.2f, 0.2f, 1f);
        glBegin(GL_LINE_LOOP);
        glVertex2f(kx - kw / 2f, s.y);
        glVertex2f(kx + kw / 2f, s.y);
        glVertex2f(kx + kw / 2f, s.y + kh);
        glVertex2f(kx - kw / 2f, s.y + kh);
        glEnd();

        glEnable(GL_TEXTURE_2D);
    }

    private void commitGamma() {
        Settings.setGamma(sGamma.value);
    }

    private void commitRenderDistance() {
        int rd = (int) sRenderDist.value;
        int prev = Settings.getRenderDistance();
        Settings.setRenderDistance(rd);
        if (Settings.getRenderDistance() != prev) {
            SocketClient.sendRenderDistance(Settings.getRenderDistance());
        }
    }

    private void onBack() {
        if (backToInGame) {
            Minecraft.mc.setScreen(null);
            if (Minecraft.mc.pauseMenu != null) {
                Minecraft.mc.pauseMenu.visible = true;
            }
            return;
        }
        if (returnScreen != null) {
            Minecraft.mc.setScreen(returnScreen);
        } else {
            Minecraft.mc.setScreen(new MenuScreen());
        }
    }
}