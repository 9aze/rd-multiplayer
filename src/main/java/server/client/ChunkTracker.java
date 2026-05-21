package server.client;

import global.Packets;
import server.Server;
import server.level.Level;
import server.level.LevelChunk;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


public class ChunkTracker {
    public static final int MAX_RENDER_DISTANCE = Server.RENDER_DISTANCE;
    public static final int VERTICAL_RENDER_DISTANCE = Server.VERTICAL_RENDER_DISTANCE;
    private static final int CHUNK_SIZE = LevelChunk.CHUNK_SIZE;

    private int renderDistance = Server.RENDER_DISTANCE;

    public void setRenderDistance(int chunks) {
        if (chunks < 2) chunks = 2;
        if (chunks > MAX_RENDER_DISTANCE) chunks = MAX_RENDER_DISTANCE;
        this.renderDistance = chunks;
    }

    private static final ConcurrentHashMap<Long, AtomicInteger> refCounts =
            new ConcurrentHashMap<>();

    private static void addRef(int cx, int cy, int cz) {
        refCounts.computeIfAbsent(pack(cx, cy, cz), k -> new AtomicInteger(0))
                 .incrementAndGet();
    }

    private static void releaseRef(int cx, int cy, int cz) {
        long key = pack(cx, cy, cz);
        AtomicInteger count = refCounts.get(key);
        if (count != null && count.decrementAndGet() <= 0) {
            refCounts.remove(key);
            Server.level.unloadChunk(cx, cy, cz);
        }
    }

    private final Set<Long> sentChunks = new HashSet<>();

    public void update(double worldX, double worldY, double worldZ, DataOutputStream out) throws IOException {
        Level level = Server.level;

        int playerCX = Math.floorDiv((int) Math.floor(worldX), CHUNK_SIZE);
        int playerCY = Math.floorDiv((int) Math.floor(worldY), CHUNK_SIZE);
        int playerCZ = Math.floorDiv((int) Math.floor(worldZ), CHUNK_SIZE);

        int minCX = playerCX - renderDistance;
        int maxCX = playerCX + renderDistance;
        int minCY = playerCY - VERTICAL_RENDER_DISTANCE;
        int maxCY = playerCY + VERTICAL_RENDER_DISTANCE;
        int minCZ = playerCZ - renderDistance;
        int maxCZ = playerCZ + renderDistance;

        // unload chunks now out of range
        Set<Long> toRemove = new HashSet<>();
        for (long key : sentChunks) {
            int cx = unpackX(key), cy = unpackY(key), cz = unpackZ(key);
            if (cx < minCX || cx > maxCX || cy < minCY || cy > maxCY || cz < minCZ || cz > maxCZ) {
                out.writeByte(Packets.CHUNK_UNLOAD);
                out.writeInt(cx);
                out.writeInt(cy);
                out.writeInt(cz);
                toRemove.add(key);
                releaseRef(cx, cy, cz);
            }
        }
        sentChunks.removeAll(toRemove);

        java.util.ArrayList<int[]> toSend = new java.util.ArrayList<>();
        for (int cx = minCX; cx <= maxCX; cx++) {
            for (int cy = minCY; cy <= maxCY; cy++) {
                for (int cz = minCZ; cz <= maxCZ; cz++) {
                    long key = pack(cx, cy, cz);
                    if (sentChunks.contains(key)) continue;
                    toSend.add(new int[]{ cx, cy, cz });
                }
            }
        }

        final int pcx = playerCX, pcy = playerCY, pcz = playerCZ;
        toSend.sort((a, b) -> {
            int adx = a[0] - pcx, ady = a[1] - pcy, adz = a[2] - pcz;
            int bdx = b[0] - pcx, bdy = b[1] - pcy, bdz = b[2] - pcz;
            int da = adx * adx + ady * ady + adz * adz;
            int db = bdx * bdx + bdy * bdy + bdz * bdz;
            return Integer.compare(da, db);
        });

        for (int[] c : toSend) {
            int cx = c[0], cy = c[1], cz = c[2];
            writeChunk(out, level, cx, cy, cz);
            sentChunks.add(pack(cx, cy, cz));
            addRef(cx, cy, cz);
        }
    }

    public boolean hasChunk(int cx, int cy, int cz) {
        return sentChunks.contains(pack(cx, cy, cz));
    }

    public void clear() {
        for (long key : sentChunks) {
            releaseRef(unpackX(key), unpackY(key), unpackZ(key));
        }
        sentChunks.clear();
    }

    private static void writeChunk(DataOutputStream out, Level level, int cx, int cy, int cz) throws IOException {
        byte[] data = level.getChunkBlocks(cx, cy, cz);
        out.writeByte(Packets.CHUNK_DATA);
        out.writeInt(cx);
        out.writeInt(cy);
        out.writeInt(cz);
        out.write(data); // 4096 bytes (16x16x16)
    }

    private static long pack(int cx, int cy, int cz) {
        return ((long)(cx & 0x1FFFFF) << 42)
             | ((long)(cy & 0x1FFFFF) << 21)
             |  (long)(cz & 0x1FFFFF);
    }
    private static int signExtend21(long v) {
        long m = v & 0x1FFFFF;
        return (int)((m & 0x100000L) != 0 ? m | ~0x1FFFFFL : m);
    }
    private static int unpackX(long key) { return signExtend21((key >> 42) & 0x1FFFFF); }
    private static int unpackY(long key) { return signExtend21((key >> 21) & 0x1FFFFF); }
    private static int unpackZ(long key) { return signExtend21( key        & 0x1FFFFF); }
}