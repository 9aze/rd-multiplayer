package server.level;

import server.Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * A cubic 16x16x16 chunk. Position is (chunkX, chunkY, chunkZ); world Y has
 * no upper or lower bound.
 *
 * Storage on disk: chunks/c_{cx}_{cy}_{cz}.dat (gzipped, 4096 raw bytes).
 */
public class LevelChunk {
    public static final int CHUNK_SIZE = 16;
    public static final int VOLUME = CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE;

    public final int chunkX;
    public final int chunkY;
    public final int chunkZ;

    public final byte[] blocks;

    private boolean dirty = false;
    /** True if every block in this chunk is air. Useful for fast skipping. */
    private boolean allAir = true;

    public LevelChunk(int chunkX, int chunkY, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;
        this.blocks = new byte[VOLUME];
    }

    private static int index(int lx, int ly, int lz) {
        return (ly * CHUNK_SIZE + lz) * CHUNK_SIZE + lx;
    }

    public byte getBlock(int lx, int ly, int lz) {
        return blocks[index(lx, ly, lz)];
    }

    public void setBlock(int lx, int ly, int lz, int id) {
        int idx = index(lx, ly, lz);
        byte old = blocks[idx];
        byte nw = (byte) id;
        if (old == nw) return;
        blocks[idx] = nw;
        dirty = true;
        if (nw != 0) allAir = false;
        // If we just turned the last block to air we won't notice here, but
        // that's fine — allAir is a fast-skip hint, not a correctness flag.
    }

    public boolean isDirty()   { return dirty; }
    public void clearDirty()   { dirty = false; }
    public void markDirty()    { dirty = true; }
    public boolean isAllAir()  { return allAir; }

    /** Called by the generator after it fills `blocks`. */
    void onGenerated() {
        recomputeAllAir();
        dirty = true;
    }

    private void recomputeAllAir() {
        for (byte b : blocks) {
            if (b != 0) { allAir = false; return; }
        }
        allAir = true;
    }

    private static Path chunkPath(Path dir, int cx, int cy, int cz) {
        return dir.resolve("c_" + cx + "_" + cy + "_" + cz + ".dat");
    }

    public boolean load(Path chunkDir) {
        Path p = chunkPath(chunkDir, chunkX, chunkY, chunkZ);
        if (!Files.exists(p)) return false;
        try (DataInputStream dis = new DataInputStream(new GZIPInputStream(Files.newInputStream(p)))) {
            dis.readFully(blocks);
            recomputeAllAir();
            dirty = false;
            return true;
        } catch (IOException e) {
            if (Server.LOGS) System.err.println("Failed to load " + p + ": " + e.getMessage());
            return false;
        }
    }

    public void save(Path chunkDir) {
        if (!dirty) return;
        Path p = chunkPath(chunkDir, chunkX, chunkY, chunkZ);
        try {
            Files.createDirectories(chunkDir);
            try (DataOutputStream dos = new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(p)))) {
                dos.write(blocks);
            }
            dirty = false;
        } catch (IOException e) {
            if (Server.LOGS) System.err.println("Failed to save " + p + ": " + e.getMessage());
        }
    }

    public void forceSave(Path chunkDir) {
        markDirty();
        save(chunkDir);
    }
}