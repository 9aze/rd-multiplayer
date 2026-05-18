package server.level;

public class LevelChunk {
    public static final int CHUNK_SIZE = 16;
    public static final int VOLUME = CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE;

    public final int chunkX;
    public final int chunkY;
    public final int chunkZ;

    public final byte[] blocks;

    private boolean dirty = false;
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
    }

    public boolean isDirty()   { return dirty; }
    public void clearDirty()   { dirty = false; }
    public void markDirty()    { dirty = true; }
    public boolean isAllAir()  { return allAir; }

    void onLoaded() {
        recomputeAllAir();
        dirty = false;
    }

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
}