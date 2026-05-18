package server.level;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;

public class Level {
    public static final int CHUNK_SIZE = LevelChunk.CHUNK_SIZE; // 16
    private static final Path CHUNK_DIR = Paths.get("chunks");
    private final ConcurrentHashMap<Long, LevelChunk> loadedChunks = new ConcurrentHashMap<>();
    private final RegionStore regionStore = new RegionStore(CHUNK_DIR);

    public Level() {}

    private static long key(int cx, int cy, int cz) {
        return ((long)(cx & 0x1FFFFF) << 42) | ((long)(cy & 0x1FFFFF) << 21) |  (long)(cz & 0x1FFFFF);
    }

    public LevelChunk getOrLoadChunk(int cx, int cy, int cz) {
        long k = key(cx, cy, cz);
        LevelChunk chunk = loadedChunks.get(k);
        if (chunk != null) return chunk;

        chunk = new LevelChunk(cx, cy, cz);
        byte[] data = regionStore.read(cx, cy, cz);
        if (data != null) {
            System.arraycopy(data, 0, chunk.blocks, 0, LevelChunk.VOLUME);
            chunk.onLoaded();
        } else {
            WorldGenerator.generate(chunk);
        }

        LevelChunk prior = loadedChunks.putIfAbsent(k, chunk);
        return prior != null ? prior : chunk;
    }

    public void unloadChunk(int cx, int cy, int cz) {
        LevelChunk chunk = loadedChunks.remove(key(cx, cy, cz));
        if (chunk != null && chunk.isDirty()) {
            regionStore.write(cx, cy, cz, chunk.blocks);
            chunk.clearDirty();
        }
    }

    public void saveAll() {
        for (LevelChunk chunk : loadedChunks.values()) {
            if (chunk.isDirty()) {
                regionStore.write(chunk.chunkX, chunk.chunkY, chunk.chunkZ, chunk.blocks);
                chunk.clearDirty();
            }
        }
        regionStore.closeAll();
        System.out.println("All chunks saved.");
    }

    public void save() { saveAll(); }

    public byte getTile(int x, int y, int z) {
        int cx = Math.floorDiv(x, CHUNK_SIZE);
        int cy = Math.floorDiv(y, CHUNK_SIZE);
        int cz = Math.floorDiv(z, CHUNK_SIZE);
        LevelChunk chunk = loadedChunks.get(key(cx, cy, cz));
        if (chunk == null) return 0;
        int lx = Math.floorMod(x, CHUNK_SIZE);
        int ly = Math.floorMod(y, CHUNK_SIZE);
        int lz = Math.floorMod(z, CHUNK_SIZE);
        return chunk.getBlock(lx, ly, lz);
    }

    public void setTile(int x, int y, int z, int id) {
        int cx = Math.floorDiv(x, CHUNK_SIZE);
        int cy = Math.floorDiv(y, CHUNK_SIZE);
        int cz = Math.floorDiv(z, CHUNK_SIZE);
        LevelChunk chunk = loadedChunks.get(key(cx, cy, cz));
        if (chunk == null) return;
        int lx = Math.floorMod(x, CHUNK_SIZE);
        int ly = Math.floorMod(y, CHUNK_SIZE);
        int lz = Math.floorMod(z, CHUNK_SIZE);
        chunk.setBlock(lx, ly, lz, id);
    }

    public byte[] getChunkBlocks(int cx, int cy, int cz) {
        return getOrLoadChunk(cx, cy, cz).blocks;
    }
}