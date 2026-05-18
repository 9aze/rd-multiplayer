package client.level;

import client.*;
import client.gfx.GL;
import client.level.block.BlockRegistry;
import client.level.block.Block;
import client.player.remote.PlayerManager;
import client.phys.AABB;
import client.player.local.LocalPlayer;
import client.player.render.PlayerRenderer;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class LevelRenderer implements LevelListener {
    private static final int CHUNK_SIZE = Level.CHUNK_SIZE;
    private final Set<Long> tntPositions = ConcurrentHashMap.newKeySet();

    private final Tessellator tessellator;
    private final Level level;
    private final PlayerRenderer playerRenderer;

    private final ConcurrentHashMap<Long, Chunk> renderChunks = new ConcurrentHashMap<>();

    private final Set<Long> pendingLoad   = ConcurrentHashMap.newKeySet();
    private final Set<Long> pendingUnload = ConcurrentHashMap.newKeySet();

    public LevelRenderer(Level level) {
        this.tessellator = new Tessellator();
        this.level = level;
        this.playerRenderer = new PlayerRenderer(tessellator);
        level.addListener(this);
    }

    private static long rcKey(int cx, int cy, int cz) {
        return ((long)(cx & 0x1FFFFF) << 42)
             | ((long)(cy & 0x1FFFFF) << 21)
             |  (long)(cz & 0x1FFFFF);
    }
    private static int signExtend21(long v) {
        long m = v & 0x1FFFFF;
        return (int)((m & 0x100000L) != 0 ? m | ~0x1FFFFFL : m);
    }
    private static int rcCX(long k) { return signExtend21((k >> 42) & 0x1FFFFF); }
    private static int rcCY(long k) { return signExtend21((k >> 21) & 0x1FFFFF); }
    private static int rcCZ(long k) { return signExtend21( k        & 0x1FFFFF); }

    @Override
    public void chunkLoaded(int cx, int cy, int cz) {
        pendingLoad.add(rcKey(cx, cy, cz));
    }

    @Override
    public void chunkUnloaded(int cx, int cy, int cz) {
        pendingUnload.add(rcKey(cx, cy, cz));
    }

    @Override
    public void lightColumnChanged(int x, int z, int minY, int maxY) {
        setDirty(x - 1, minY - 1, z - 1, x + 1, maxY + 1, z + 1);
    }

    @Override
    public void tileChanged(int x, int y, int z) {
        setDirty(x - 1, y - 1, z - 1, x + 1, y + 1, z + 1);

        long tntKey = packTnt(x, y, z);
        if ((level.getRawBlock(x, y, z) & 0xFF) == BlockRegistry.TNT.id) {
            tntPositions.add(tntKey);
        } else {
            tntPositions.remove(tntKey);
        }
    }

    @Override
    public void allChanged() {
        for (Chunk rc : renderChunks.values()) rc.setDirty();
    }

    private void applyPendingChunks() {
        for (java.util.Iterator<Long> it = pendingUnload.iterator(); it.hasNext(); ) {
            long key = it.next();
            it.remove();
            Chunk rc = renderChunks.remove(key);
            if (rc != null) rc.dispose();
            int cx = rcCX(key), cy = rcCY(key), cz = rcCZ(key);
            markNeighborsDirty(cx, cy, cz);
        }

        for (java.util.Iterator<Long> it = pendingLoad.iterator(); it.hasNext(); ) {
            long key = it.next();
            it.remove();
            int cx = rcCX(key), cy = rcCY(key), cz = rcCZ(key);
            createRenderChunk(cx, cy, cz);
            markNeighborsDirty(cx, cy, cz);
        }
    }

    private void markNeighborsDirty(int cx, int cy, int cz) {
        markIfPresent(cx - 1, cy, cz);
        markIfPresent(cx + 1, cy, cz);
        markIfPresent(cx, cy - 1, cz);
        markIfPresent(cx, cy + 1, cz);
        markIfPresent(cx, cy, cz - 1);
        markIfPresent(cx, cy, cz + 1);
    }

    private void markIfPresent(int cx, int cy, int cz) {
        Chunk rc = renderChunks.get(rcKey(cx, cy, cz));
        if (rc != null) rc.setDirty();
    }

    private void createRenderChunk(int cx, int cy, int cz) {
        long key = rcKey(cx, cy, cz);
        Chunk rc = renderChunks.get(key);
        if (rc == null) {
            rc = new Chunk(level, cx, cy, cz);
            renderChunks.put(key, rc);

            int minX = cx * CHUNK_SIZE, minY = cy * CHUNK_SIZE, minZ = cz * CHUNK_SIZE;
            int maxX = minX + CHUNK_SIZE, maxY = minY + CHUNK_SIZE, maxZ = minZ + CHUNK_SIZE;
            for (int x = minX; x < maxX; x++)
                for (int y = minY; y < maxY; y++)
                    for (int z = minZ; z < maxZ; z++)
                        if ((level.getRawBlock(x, y, z) & 0xFF) == BlockRegistry.TNT.id)
                            tntPositions.add(packTnt(x, y, z));
        }
        rc.setDirty();
    }

    public void setDirty(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        int minCX = Math.floorDiv(minX, CHUNK_SIZE);
        int minCY = Math.floorDiv(minY, CHUNK_SIZE);
        int minCZ = Math.floorDiv(minZ, CHUNK_SIZE);
        int maxCX = Math.floorDiv(maxX, CHUNK_SIZE);
        int maxCY = Math.floorDiv(maxY, CHUNK_SIZE);
        int maxCZ = Math.floorDiv(maxZ, CHUNK_SIZE);

        for (Map.Entry<Long, Chunk> e : renderChunks.entrySet()) {
            long k = e.getKey();
            int cx = rcCX(k), cy = rcCY(k), cz = rcCZ(k);
            if (cx >= minCX && cx <= maxCX
                    && cy >= minCY && cy <= maxCY
                    && cz >= minCZ && cz <= maxCZ) {
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

    public void pick(LocalPlayer localPlayer) {
        float radius = 3.0F;
        AABB bb = localPlayer.boundingBox.grow(radius, radius, radius);

        int x0 = (int) bb.minX, x1 = (int) (bb.maxX + 1);
        int y0 = (int) bb.minY, y1 = (int) (bb.maxY + 1);
        int z0 = (int) bb.minZ, z1 = (int) (bb.maxZ + 1);

        GL.initNames();
        for (int x = x0; x < x1; x++) {
            GL.pushName(x);
            for (int y = y0; y < y1; y++) {
                GL.pushName(y);
                for (int z = z0; z < z1; z++) {
                    GL.pushName(z);
                    if (level.isSolidTile(x, y, z)) {
                        int blockId = level.getRawBlock(x, y, z) & 0xFF;
                        Block block = BlockRegistry.get(blockId);
                        if (block != null) {
                            GL.pushName(0);
                            for (int face = 0; face < 6; face++) {
                                GL.pushName(face);
                                tessellator.init();
                                block.renderFace(tessellator, x, y, z, face);
                                tessellator.flush();
                                GL.popName();
                            }
                            GL.popName();
                        }
                    }
                    GL.popName();
                }
                GL.popName();
            }
            GL.popName();
        }
    }

    public void renderHit(HitResult hitResult) {
        int blockId = level.getRawBlock(hitResult.x, hitResult.y, hitResult.z) & 0xFF;
        Block block = BlockRegistry.get(blockId);
        if (block == null) return;

        GL.enable(GL.BLEND);
        GL.blendFunc(GL.SRC_ALPHA, GL.CURRENT_BIT);
        GL.color4f(1f, 1f, 1f,
                (float) Math.sin(System.currentTimeMillis() / 100.0) * 0.2f + 0.4f);
        tessellator.init();
        block.renderFace(tessellator, hitResult.x, hitResult.y, hitResult.z, hitResult.face);
        tessellator.flush();
        GL.disable(GL.BLEND);
    }

    public void renderPlayers(PlayerManager playerManager) {
        playerRenderer.renderPlayers(playerManager);
    }

    public void renderSelf(LocalPlayer p, PlayerManager playerManager) {
        playerRenderer.renderSelf(p, playerManager);
    }

    public void renderNameTags(PlayerManager playerManager, LocalPlayer localPlayer, FontRenderer fontRenderer) {
        playerRenderer.renderNameTags(playerManager, localPlayer, fontRenderer);
    }

    private static long packTnt(int x, int y, int z) {
        return ((long)(x & 0x1FFFFF) << 42)
             | ((long)(y & 0x1FFFFF) << 21)
             |  (long)(z & 0x1FFFFF);
    }
    private static int unpackTntX(long k) { return signExtend21((k >> 42) & 0x1FFFFF); }
    private static int unpackTntY(long k) { return signExtend21((k >> 21) & 0x1FFFFF); }
    private static int unpackTntZ(long k) { return signExtend21( k        & 0x1FFFFF); }

    public void renderTntOverlay() {
        if (tntPositions.isEmpty()) return;
        float alpha = (float)(Math.sin(System.currentTimeMillis() / 150.0) * 0.5 + 0.5) * 0.8f;

        GL.enable(GL.BLEND);
        GL.blendFunc(GL.SRC_ALPHA, GL.ONE_MINUS_SRC_ALPHA);
        GL.disable(GL.TEXTURE_2D);
        GL.disable(GL.LIGHTING);
        GL.color4f(1f, 1f, 1f, alpha);

        Frustum frustum = Frustum.getFrustum();

        for (long key : tntPositions) {
            int x = unpackTntX(key);
            int y = unpackTntY(key);
            int z = unpackTntZ(key);

            if (!frustum.cubeInFrustum(x, y, z, x + 1, y + 1, z + 1)) continue;

            float x0 = x, x1 = x + 1;
            float y0 = y, y1 = y + 1;
            float z0 = z, z1 = z + 1;

            GL.begin(GL.QUADS);
            if (!level.isSolidTile(x, y - 1, z)) {
                GL.vertex3f(x0,y0,z1); GL.vertex3f(x0,y0,z0); GL.vertex3f(x1,y0,z0); GL.vertex3f(x1,y0,z1);
            }
            if (!level.isSolidTile(x, y + 1, z)) {
                GL.vertex3f(x1,y1,z1); GL.vertex3f(x1,y1,z0); GL.vertex3f(x0,y1,z0); GL.vertex3f(x0,y1,z1);
            }
            if (!level.isSolidTile(x, y, z - 1)) {
                GL.vertex3f(x0,y1,z0); GL.vertex3f(x1,y1,z0); GL.vertex3f(x1,y0,z0); GL.vertex3f(x0,y0,z0);
            }
            if (!level.isSolidTile(x, y, z + 1)) {
                GL.vertex3f(x0,y1,z1); GL.vertex3f(x0,y0,z1); GL.vertex3f(x1,y0,z1); GL.vertex3f(x1,y1,z1);
            }
            if (!level.isSolidTile(x - 1, y, z)) {
                GL.vertex3f(x0,y1,z1); GL.vertex3f(x0,y1,z0); GL.vertex3f(x0,y0,z0); GL.vertex3f(x0,y0,z1);
            }
            if (!level.isSolidTile(x + 1, y, z)) {
                GL.vertex3f(x1,y0,z1); GL.vertex3f(x1,y0,z0); GL.vertex3f(x1,y1,z0); GL.vertex3f(x1,y1,z1);
            }
            GL.end();
        }

        GL.enable(GL.TEXTURE_2D);
        GL.enable(GL.LIGHTING);
        GL.disable(GL.BLEND);
        GL.color4f(1f, 1f, 1f, 1f);
    }
}