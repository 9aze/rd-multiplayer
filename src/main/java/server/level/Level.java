package server.level;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;

public class Level {

    public static final int CHUNK_SIZE = LevelChunk.CHUNK_SIZE; // 16

    private static final Path CHUNK_DIR = Paths.get("chunks");

    private final ConcurrentHashMap<Long, LevelChunk> loadedChunks = new ConcurrentHashMap<>();

    public Level() {}

    /**
     * Pack (cx, cy, cz) into a single long. Each component is signed 21 bits,
     * range [-2^20, 2^20-1] = ±1,048,575 chunks = ±16.7M blocks. Vastly more
     * than enough for any reasonable play area.
     */
    private static long key(int cx, int cy, int cz) {
        return ((long)(cx & 0x1FFFFF) << 42)
             | ((long)(cy & 0x1FFFFF) << 21)
             |  (long)(cz & 0x1FFFFF);
    }

    public LevelChunk getOrLoadChunk(int cx, int cy, int cz) {
        long k = key(cx, cy, cz);
        LevelChunk chunk = loadedChunks.get(k);
        if (chunk != null) return chunk;

        chunk = new LevelChunk(cx, cy, cz);
        if (!chunk.load(CHUNK_DIR)) {
            WorldGenerator.generate(chunk);
        }
        // Concurrency: another thread might have loaded the same chunk in
        // parallel. putIfAbsent and return whichever wins.
        LevelChunk prior = loadedChunks.putIfAbsent(k, chunk);
        return prior != null ? prior : chunk;
    }

    public void unloadChunk(int cx, int cy, int cz) {
        LevelChunk chunk = loadedChunks.remove(key(cx, cy, cz));
        if (chunk != null) {
            chunk.save(CHUNK_DIR);
        }
    }

    public void saveAll() {
        for (LevelChunk chunk : loadedChunks.values()) {
            chunk.save(CHUNK_DIR);
        }
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