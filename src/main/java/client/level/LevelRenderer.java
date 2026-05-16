package client.level;

import client.*;
import client.net.PlayerManager;
import client.phys.AABB;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.lwjgl.opengl.GL11.*;

public class LevelRenderer implements LevelListener {

    private static final int CHUNK_SIZE = Level.CHUNK_SIZE;
    private static final int RENDER_CHUNK_HEIGHT = 16;

    private final Tessellator tessellator;
    private final Level level;

    private final ConcurrentHashMap<Long, Chunk> renderChunks = new ConcurrentHashMap<>();

    private final Set<Long> pendingLoad   = ConcurrentHashMap.newKeySet();
    private final Set<Long> pendingUnload = ConcurrentHashMap.newKeySet();

    public LevelRenderer(Level level) {
        this.tessellator = new Tessellator();
        this.level = level;
        level.addListener(this);
    }

    private static long rcKey(int cx, int sliceY, int cz) {
        return ((long)(cx & 0xFFFFL) << 32) | ((long)(sliceY & 0xFFFFL) << 16) | (cz & 0xFFFFL);
    }

    private static int rcCX(long key) { return (short)((key >> 32) & 0xFFFFL); }
    private static int rcSY(long key) { return (int)  ((key >> 16) & 0xFFFFL); }
    private static int rcCZ(long key) { return (short)( key        & 0xFFFFL); }

    private static long colKey(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }
    private static int colCX(long key) { return (int)(key >> 32); }
    private static int colCZ(long key) { return (int) key; }

    private int sliceCount() {
        return Math.max(1, (level.depth + RENDER_CHUNK_HEIGHT - 1) / RENDER_CHUNK_HEIGHT);
    }

    @Override
    public void chunkLoaded(int cx, int cz) {
        pendingLoad.add(colKey(cx, cz));
    }

    @Override
    public void chunkUnloaded(int cx, int cz) {
        pendingUnload.add(colKey(cx, cz));
    }

    @Override
    public void lightColumnChanged(int x, int z, int minY, int maxY) {
        setDirty(x - 1, minY - 1, z - 1, x + 1, maxY + 1, z + 1);
    }

    @Override
    public void tileChanged(int x, int y, int z) {
        setDirty(x - 1, y - 1, z - 1, x + 1, y + 1, z + 1);
    }

    @Override
    public void allChanged() {
        for (Chunk rc : renderChunks.values()) rc.setDirty();
    }

    private void applyPendingChunks() {
        for (long key : pendingUnload) {
            int cx = colCX(key);
            int cz = colCZ(key);
            int slices = sliceCount();
            for (int sy = 0; sy < slices; sy++) {
                renderChunks.remove(rcKey(cx, sy, cz));
            }
        }
        pendingUnload.clear();

        for (long key : pendingLoad) {
            int cx = colCX(key);
            int cz = colCZ(key);
            createRenderChunks(cx, cz);
        }
        pendingLoad.clear();
    }

    private void createRenderChunks(int cx, int cz) {
        int slices = sliceCount();
        for (int sy = 0; sy < slices; sy++) {
            long key = rcKey(cx, sy, cz);
            Chunk rc = renderChunks.get(key);
            if (rc == null) {
                int minX = cx * CHUNK_SIZE;
                int minY = sy * RENDER_CHUNK_HEIGHT;
                int minZ = cz * CHUNK_SIZE;
                int maxX = minX + CHUNK_SIZE;
                int maxY = Math.min(level.depth, minY + RENDER_CHUNK_HEIGHT);
                int maxZ = minZ + CHUNK_SIZE;
                rc = new Chunk(level, minX, minY, minZ, maxX, maxY, maxZ);
                renderChunks.put(key, rc);
            }
            rc.setDirty();
        }
    }

    public void setDirty(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        int minCX = Math.floorDiv(minX, CHUNK_SIZE);
        int minSY = Math.max(0, Math.floorDiv(minY, RENDER_CHUNK_HEIGHT));
        int minCZ = Math.floorDiv(minZ, CHUNK_SIZE);
        int maxCX = Math.floorDiv(maxX, CHUNK_SIZE);
        int maxSY = Math.floorDiv(maxY, RENDER_CHUNK_HEIGHT);
        int maxCZ = Math.floorDiv(maxZ, CHUNK_SIZE);

        for (Map.Entry<Long, Chunk> e : renderChunks.entrySet()) {
            long k  = e.getKey();
            int  cx = rcCX(k);
            int  sy = rcSY(k);
            int  cz = rcCZ(k);
            if (cx >= minCX && cx <= maxCX && sy >= minSY && sy <= maxSY && cz >= minCZ && cz <= maxCZ) {
                e.getValue().setDirty();
            }
        }
    }

    public void render(int layer) {
        if (layer == 0) {
            applyPendingChunks();
            Chunk.rebuiltThisFrame = 0;
        }

        Frustum frustum = Frustum.getFrustum();
        for (Chunk rc : renderChunks.values()) {
            if (frustum.cubeInFrustum(rc.boundingBox)) {
                rc.render(layer);
            }
        }
    }

    public void rebuildAll() {
        applyPendingChunks();
        for (Chunk rc : renderChunks.values()) {
            rc.rebuildNow(0);
            rc.rebuildNow(1);
        }
    }

    public void pick(Player player) {
        float radius = 3.0F;
        AABB bb = player.boundingBox.grow(radius, radius, radius);

        int x0 = (int) bb.minX, x1 = (int)(bb.maxX + 1);
        int y0 = (int) bb.minY, y1 = (int)(bb.maxY + 1);
        int z0 = (int) bb.minZ, z1 = (int)(bb.maxZ + 1);

        glInitNames();
        for (int x = x0; x < x1; x++) {
            glPushName(x);
            for (int y = y0; y < y1; y++) {
                glPushName(y);
                for (int z = z0; z < z1; z++) {
                    glPushName(z);
                    if (level.isSolidTile(x, y, z)) {
                        int blockId = level.getRawBlock(x, y, z) & 0xFF;
                        Tile tile = Blocks.get(blockId);
                        if (tile != null) {
                            glPushName(0);
                            for (int face = 0; face < 6; face++) {
                                glPushName(face);
                                tessellator.init();
                                tile.renderFace(tessellator, x, y, z, face);
                                tessellator.flush();
                                glPopName();
                            }
                            glPopName();
                        }
                    }
                    glPopName();
                }
                glPopName();
            }
            glPopName();
        }
    }

    public void renderHit(HitResult hitResult) {
        int blockId = level.getRawBlock(hitResult.x, hitResult.y, hitResult.z) & 0xFF;
        Tile tile = Blocks.get(blockId);
        if (tile == null) return;

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_CURRENT_BIT);
        glColor4f(1f, 1f, 1f,
                (float) Math.sin(System.currentTimeMillis() / 100.0) * 0.2f + 0.4f);
        tessellator.init();
        tile.renderFace(tessellator, hitResult.x, hitResult.y, hitResult.z, hitResult.face);
        tessellator.flush();
        glDisable(GL_BLEND);
    }

    public void renderPlayers(PlayerManager playerManager) {
        long now = System.currentTimeMillis();

        java.util.List<java.util.Map.Entry<String, client.Position>> snapshot;
        synchronized (playerManager) {
            snapshot = new java.util.ArrayList<>(playerManager.getPlayers().entrySet());
        }

        glDisable(GL_CULL_FACE); glDisable(GL_FOG);
        glEnable(GL_BLEND); glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        for (java.util.Map.Entry<String, client.Position> entry : snapshot) {
            client.Position pos = entry.getValue();
            uploadPendingSkin(pos);
            updateAnimation(pos, now);

            bindPlayerSkin(pos.skinTextureId);
            glPushMatrix();
            glTranslatef((float) pos.x, (float) pos.y - 1.62f, (float) pos.z);
            glRotatef(-pos.yaw, 0f, 1f, 0f);
            renderPlayerModel(pos.limbSwing, pos.limbSwingAmount, pos.pitch, pos.skinTextureId);
            glPopMatrix();
        }

        Textures.bind(0);
        glDisable(GL_BLEND); glEnable(GL_CULL_FACE); glEnable(GL_FOG); glEnable(GL_TEXTURE_2D);
    }

    private final client.Position selfPosition = new client.Position(0, 0, 0, 0f, 0);

    public void renderSelf(Player p, PlayerManager playerManager) {
        selfPosition.x = p.x;
        selfPosition.y = p.y;
        selfPosition.z = p.z;
        selfPosition.yaw = p.yRotation;
        selfPosition.pitch = p.xRotation;

        client.Position mine;
        synchronized (playerManager) {
            mine = playerManager.getPlayers().get(client.Minecraft.mc.username);
        }
        if (mine != null) {
            if (mine.pendingSkinPng != null) {
                selfPosition.pendingSkinPng = mine.pendingSkinPng;
                mine.pendingSkinPng = null;
            }
            if (mine.skinTextureId != -1) {
                selfPosition.skinTextureId = mine.skinTextureId;
            }
        }
        uploadPendingSkin(selfPosition);

        long now = System.currentTimeMillis();
        updateAnimation(selfPosition, now);

        glDisable(GL_CULL_FACE); glDisable(GL_FOG);
        glEnable(GL_BLEND); glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        bindPlayerSkin(selfPosition.skinTextureId);
        glPushMatrix();
        glTranslatef((float) p.x, (float) p.y - 1.62f, (float) p.z);
        glRotatef(-p.yRotation, 0f, 1f, 0f);
        renderPlayerModel(selfPosition.limbSwing, selfPosition.limbSwingAmount,
                          selfPosition.pitch, selfPosition.skinTextureId);
        glPopMatrix();

        Textures.bind(0);
        glDisable(GL_BLEND); glEnable(GL_CULL_FACE); glEnable(GL_FOG); glEnable(GL_TEXTURE_2D);
    }

    private void bindPlayerSkin(int skinTextureId) {
        if (skinTextureId != -1) {
            glEnable(GL_TEXTURE_2D);
            Textures.bind(skinTextureId);
        } else {
            glDisable(GL_TEXTURE_2D);
        }
    }

    private void uploadPendingSkin(client.Position pos) {
        byte[] png = pos.pendingSkinPng;
        if (png == null) return;
        pos.pendingSkinPng = null;

        try {
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(
                    new java.io.ByteArrayInputStream(png));
            if (img == null) {
                System.err.println("Skin upload failed: ImageIO returned null");
                return;
            }
            int w = img.getWidth(), h = img.getHeight();
            if (!((w == 64 && h == 64) || (w == 64 && h == 32))) {
                System.err.println("Skin upload rejected: bad size " + w + "x" + h);
                return;
            }

            int[] pixels = new int[w * h];
            img.getRGB(0, 0, w, h, pixels, 0, w);
            for (int i = 0; i < pixels.length; i++) {
                int a = (pixels[i] >> 24) & 0xFF;
                int r = (pixels[i] >> 16) & 0xFF;
                int g = (pixels[i] >>  8) & 0xFF;
                int b =  pixels[i] & 0xFF;
                pixels[i] = (a << 24) | (b << 16) | (g << 8) | r;
            }
            java.nio.ByteBuffer buf = org.lwjgl.BufferUtils.createByteBuffer(w * h * 4);
            buf.asIntBuffer().put(pixels);

            int id = (pos.skinTextureId != -1) ? pos.skinTextureId : glGenTextures();
            glBindTexture(GL_TEXTURE_2D, id);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf);
            pos.skinTextureId = id;
        } catch (Exception e) {
            System.err.println("Skin decode/upload failed: " + e.getMessage());
        }
    }

    /** Walk-cycle constants — tune to taste. */
    private static final float SWING_SPEED    = 0.33f;    // phase advance per frame at full motion
    private static final float SWING_AMP      = 0.9f;     // peak swing in radians (~52°)
    private static final float SWING_EASE     = 0.4f;     // how fast amount eases toward target
    private static final float MOVE_THRESHOLD = 0.05f;    // blocks moved per frame to count as "moving"

    private static void updateAnimation(client.Position pos, long now) {
        // First frame for this player — initialise and bail.
        if (pos.lastAnimTime == 0) {
            pos.lastAnimTime = now;
            pos.prevAnimX = pos.x;
            pos.prevAnimZ = pos.z;
            return;
        }

        double dx = pos.x - pos.prevAnimX;
        double dz = pos.z - pos.prevAnimZ;
        double moved = Math.sqrt(dx * dx + dz * dz);

        // Ease limbSwingAmount toward 1.0 if moving, 0.0 if still.
        float target = (moved > MOVE_THRESHOLD) ? 1f : 0f;
        pos.limbSwingAmount += (target - pos.limbSwingAmount) * SWING_EASE;

        // Advance the swing phase proportional to how strongly limbs are swinging.
        pos.limbSwing += SWING_SPEED * pos.limbSwingAmount;

        pos.prevAnimX = pos.x;
        pos.prevAnimZ = pos.z;
        pos.lastAnimTime = now;
    }

    private void renderPlayerModel(float limbSwing, float limbSwingAmount, float pitch, int skin) {
        float swing = (float) Math.sin(limbSwing) * SWING_AMP * limbSwingAmount;
        float swingDeg = (float) Math.toDegrees(swing);

        boolean textured = skin != -1;

        // Body — stationary, batched on its own.
        tessellator.init();
        if (textured) {
            renderSkinBox(-0.25f, 0.60f, -0.125f, 0.25f, 1.35f, 0.125f,
                          client.SkinModel.BODY);
        } else {
            renderBox(-0.25f, 0.60f, -0.125f, 0.25f, 1.35f, 0.125f, 0.22f, 0.40f, 0.75f);
        }
        tessellator.flush();

        // Body outer (jacket) — slightly larger, transparent. Skin only.
        if (textured) {
            glDepthMask(false);
            tessellator.init();
            renderSkinBoxScaled(-0.25f, 0.60f, -0.125f, 0.25f, 1.35f, 0.125f,
                                client.SkinModel.BODY_OUTER, 0.03125f);
            tessellator.flush();
            glDepthMask(true);
        }

        // Head pivots at the top of the body (y = 1.35). Yaw is already
        // applied at the parent transform; here we just add pitch.
        renderHead(textured ? skin : -1, pitch);

        // Right leg — pivots at hip (top of leg, y = 0.60).
        renderLimbBox( 0.00f, 0.00f, -0.125f, 0.25f, 0.60f, 0.125f,
                       textured ? client.SkinModel.R_LEG : null,
                       textured ? client.SkinModel.R_LEG_OUTER : null,
                       0.15f, 0.25f, 0.55f,  0.60f,  swingDeg);

        // Left leg — opposite phase.
        renderLimbBox(-0.25f, 0.00f, -0.125f, 0.00f, 0.60f, 0.125f,
                       textured ? client.SkinModel.L_LEG : null,
                       textured ? client.SkinModel.L_LEG_OUTER : null,
                       0.15f, 0.25f, 0.55f,  0.60f, -swingDeg);

        // Right arm — pivots at shoulder (top of arm, y = 1.35).
        // Width 0.1875 (= 3 vanilla pixels, "slim" arm), inner edge flush with body.
        renderLimbBox( 0.25f, 0.60f, -0.125f, 0.4375f, 1.35f, 0.125f,
                       textured ? client.SkinModel.R_ARM : null,
                       textured ? client.SkinModel.R_ARM_OUTER : null,
                       0.85f, 0.65f, 0.50f,  1.35f, -swingDeg);

        // Left arm — same phase as right leg.
        renderLimbBox(-0.4375f, 0.60f, -0.125f,-0.25f, 1.35f, 0.125f,
                       textured ? client.SkinModel.L_ARM : null,
                       textured ? client.SkinModel.L_ARM_OUTER : null,
                       0.85f, 0.65f, 0.50f,  1.35f,  swingDeg);
    }

    private void renderHead(int skin, float pitch) {
        float pivotY = 1.35f;
        // Clamp pitch (anti-cheat already caps at ±90 but be safe).
        float p = Math.max(-90f, Math.min(90f, pitch));

        glPushMatrix();
        glTranslatef(0f, pivotY, 0f);
        // xRotation is the camera's world-rotation angle; positive means the
        // camera is looking *down* (the world tilts up around the camera).
        // To make the head model tilt the same way the player is looking,
        // we apply the opposite sign.
        glRotatef(-p, 1f, 0f, 0f);
        glTranslatef(0f, -pivotY, 0f);

        tessellator.init();
        if (skin != -1) {
            renderSkinBox(-0.25f, 1.35f, -0.25f, 0.25f, 1.85f, 0.25f,
                          client.SkinModel.HEAD);
        } else {
            renderBox(-0.25f, 1.35f, -0.25f,  0.25f, 1.85f,  0.25f,  0.85f, 0.65f, 0.50f);
            renderPlayerFace();
        }
        tessellator.flush();

        // Hat layer (head outer) — slightly larger, transparent.
        if (skin != -1) {
            glDepthMask(false);
            tessellator.init();
            renderSkinBoxScaled(-0.25f, 1.35f, -0.25f, 0.25f, 1.85f, 0.25f,
                                client.SkinModel.HEAD_HAT, 0.0625f);
            tessellator.flush();
            glDepthMask(true);
        }

        glPopMatrix();
    }

    /**
     * Render an animated limb (leg or arm) — rotates the box around its
     * top (pivotY) so it swings about the hip/shoulder. Uses skin textures
     * if {@code uvBase} is non-null, otherwise falls back to flat color (r,g,b).
     */
    private void renderLimbBox(float x0, float y0, float z0,
                               float x1, float y1, float z1,
                               float[][] uvBase, float[][] uvOuter,
                               float r,  float g,  float b,
                               float pivotY, float angleDeg) {
        glPushMatrix();
        glTranslatef(0f, pivotY, 0f);
        glRotatef(angleDeg, 1f, 0f, 0f);
        glTranslatef(0f, -pivotY, 0f);

        tessellator.init();
        if (uvBase != null) {
            renderSkinBox(x0, y0, z0, x1, y1, z1, uvBase);
        } else {
            renderBox(x0, y0, z0, x1, y1, z1, r, g, b);
        }
        tessellator.flush();

        if (uvOuter != null) {
            glDepthMask(false);
            tessellator.init();
            renderSkinBoxScaled(x0, y0, z0, x1, y1, z1, uvOuter, 0.03125f);
            tessellator.flush();
            glDepthMask(true);
        }

        glPopMatrix();
    }

    private void renderPlayerFace() {
        float z = -0.251f;

        renderFaceQuad(-0.15f, 1.72f, -0.03f, 1.65f, z,  1.0f, 1.0f, 1.0f);
        renderFaceQuad( 0.03f, 1.72f,  0.15f, 1.65f, z,  1.0f, 1.0f, 1.0f);
        renderFaceQuad(-0.13f, 1.70f, -0.07f, 1.66f, z,  0.08f, 0.08f, 0.08f);
        renderFaceQuad( 0.07f, 1.70f,  0.13f, 1.66f, z,  0.08f, 0.08f, 0.08f);
        renderFaceQuad(-0.10f, 1.47f,  0.10f, 1.44f, z,  0.25f, 0.08f, 0.08f);
    }

    private void renderFaceQuad(float x0, float y0, float x1, float y1, float z,
                                float r, float g, float b) {
        tessellator.color(r, g, b);
        tessellator.vertex(x0, y0, z);
        tessellator.vertex(x1, y0, z);
        tessellator.vertex(x1, y1, z);
        tessellator.vertex(x0, y1, z);
    }

    private void renderBox(float x0, float y0, float z0, float x1, float y1, float z1,
                           float r, float g, float b) {
        tessellator.color(r, g, b);
        tessellator.vertex(x0, y0, z0); tessellator.vertex(x1, y0, z0);
        tessellator.vertex(x1, y0, z1); tessellator.vertex(x0, y0, z1);
        tessellator.vertex(x0, y1, z0); tessellator.vertex(x1, y1, z0);
        tessellator.vertex(x1, y1, z1); tessellator.vertex(x0, y1, z1);
        tessellator.vertex(x0, y0, z0); tessellator.vertex(x1, y0, z0);
        tessellator.vertex(x1, y1, z0); tessellator.vertex(x0, y1, z0);
        tessellator.vertex(x0, y0, z1); tessellator.vertex(x1, y0, z1);
        tessellator.vertex(x1, y1, z1); tessellator.vertex(x0, y1, z1);
        tessellator.vertex(x0, y0, z0); tessellator.vertex(x0, y1, z0);
        tessellator.vertex(x0, y1, z1); tessellator.vertex(x0, y0, z1);
        tessellator.vertex(x1, y0, z0); tessellator.vertex(x1, y1, z0);
        tessellator.vertex(x1, y1, z1); tessellator.vertex(x1, y0, z1);
    }

    /**
     * Render a textured box using a {@link client.SkinModel}-style UV table.
     * Vertex emission order matches {@link #renderBox}; UVs per corner per
     * face are picked so the standard Minecraft skin unwrap maps correctly.
     */
    private void renderSkinBox(float x0, float y0, float z0,
                               float x1, float y1, float z1,
                               float[][] uv) {
        tessellator.color(1f, 1f, 1f);

        // BOTTOM (-Y) — face 0
        float u0 = uv[0][0], v0 = uv[0][1], u1 = uv[0][2], v1 = uv[0][3];
        tessellator.texture(u0, v0); tessellator.vertex(x0, y0, z0);
        tessellator.texture(u1, v0); tessellator.vertex(x1, y0, z0);
        tessellator.texture(u1, v1); tessellator.vertex(x1, y0, z1);
        tessellator.texture(u0, v1); tessellator.vertex(x0, y0, z1);

        // TOP (+Y) — face 1
        u0 = uv[1][0]; v0 = uv[1][1]; u1 = uv[1][2]; v1 = uv[1][3];
        tessellator.texture(u0, v1); tessellator.vertex(x0, y1, z0);
        tessellator.texture(u1, v1); tessellator.vertex(x1, y1, z0);
        tessellator.texture(u1, v0); tessellator.vertex(x1, y1, z1);
        tessellator.texture(u0, v0); tessellator.vertex(x0, y1, z1);

        // NORTH (-Z) — face 2 (player's front)
        u0 = uv[2][0]; v0 = uv[2][1]; u1 = uv[2][2]; v1 = uv[2][3];
        tessellator.texture(u1, v1); tessellator.vertex(x0, y0, z0);
        tessellator.texture(u0, v1); tessellator.vertex(x1, y0, z0);
        tessellator.texture(u0, v0); tessellator.vertex(x1, y1, z0);
        tessellator.texture(u1, v0); tessellator.vertex(x0, y1, z0);

        // SOUTH (+Z) — face 3 (player's back)
        u0 = uv[3][0]; v0 = uv[3][1]; u1 = uv[3][2]; v1 = uv[3][3];
        tessellator.texture(u0, v1); tessellator.vertex(x0, y0, z1);
        tessellator.texture(u1, v1); tessellator.vertex(x1, y0, z1);
        tessellator.texture(u1, v0); tessellator.vertex(x1, y1, z1);
        tessellator.texture(u0, v0); tessellator.vertex(x0, y1, z1);

        // WEST (-X) — face 4 (player's left side)
        u0 = uv[4][0]; v0 = uv[4][1]; u1 = uv[4][2]; v1 = uv[4][3];
        tessellator.texture(u0, v1); tessellator.vertex(x0, y0, z0);
        tessellator.texture(u0, v0); tessellator.vertex(x0, y1, z0);
        tessellator.texture(u1, v0); tessellator.vertex(x0, y1, z1);
        tessellator.texture(u1, v1); tessellator.vertex(x0, y0, z1);

        // EAST (+X) — face 5 (player's right side)
        u0 = uv[5][0]; v0 = uv[5][1]; u1 = uv[5][2]; v1 = uv[5][3];
        tessellator.texture(u1, v1); tessellator.vertex(x1, y0, z0);
        tessellator.texture(u1, v0); tessellator.vertex(x1, y1, z0);
        tessellator.texture(u0, v0); tessellator.vertex(x1, y1, z1);
        tessellator.texture(u0, v1); tessellator.vertex(x1, y0, z1);
    }

    private void renderSkinBoxScaled(float x0, float y0, float z0, float x1, float y1, float z1, float[][] uv, float grow) {
        renderSkinBox(x0 - grow, y0 - grow, z0 - grow, x1 + grow, y1 + grow, z1 + grow, uv);
    }


    public void renderNameTags(PlayerManager playerManager, Player localPlayer, FontRenderer fontRenderer) {
        glEnable(GL_DEPTH_TEST); glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_TEXTURE_2D); glDisable(GL_FOG);
        glDepthMask(false); glDisable(GL_CULL_FACE);

        for (Map.Entry<String, client.Position> entry : playerManager.getPlayers().entrySet()) {
            String name = entry.getKey();
            client.Position pos = entry.getValue();
            glPushMatrix();
            glTranslated(pos.x, pos.y + 0.7D, pos.z);
            glRotatef(-localPlayer.yRotation, 0f, 1f, 0f);
            glRotatef(localPlayer.xRotation,  1f, 0f, 0f);
            float scale = 0.015F;
            glScalef(scale, -scale, scale);
            int tw = fontRenderer.getStringWidth(name);
            int th = fontRenderer.getStringHeight();
            int xo = -tw / 2;
            glDisable(GL_TEXTURE_2D);
            glColor4f(0f, 0f, 0f, 0.25f);
            glBegin(GL_QUADS);
            glVertex3f(xo - 2, -1, 0);  glVertex3f(xo + tw + 2, -1, 0);
            glVertex3f(xo + tw + 2, th + 1, 0); glVertex3f(xo - 2, th + 1, 0);
            glEnd();
            glEnable(GL_TEXTURE_2D);
            glColor4f(1f, 1f, 1f, 1f);
            fontRenderer.drawString(name, xo, 0, true);
            Textures.bind(0);
            glPopMatrix();
        }

        glDepthMask(true); glEnable(GL_CULL_FACE); glEnable(GL_FOG); glDisable(GL_BLEND);
    }
}