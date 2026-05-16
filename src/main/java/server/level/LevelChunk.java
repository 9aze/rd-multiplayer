package server.level;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class LevelChunk {
    public static final int CHUNK_SIZE = 16;

    public final int chunkX;
    public final int chunkZ;
    public final int depth;

    public final byte[] blocks;

    private boolean dirty = false;

    public LevelChunk(int chunkX, int chunkZ, int depth) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.depth = depth;
        this.blocks = new byte[CHUNK_SIZE * CHUNK_SIZE * depth];
    }

    private int index(int lx, int y, int lz) {
        return (y * CHUNK_SIZE + lz) * CHUNK_SIZE + lx;
    }

    public byte getBlock(int lx, int y, int lz) {
        return blocks[index(lx, y, lz)];
    }

    public void setBlock(int lx, int y, int lz, int id) {
        blocks[index(lx, y, lz)] = (byte) id;
        dirty = true;
    }

    public boolean isDirty() { return dirty; }
    public void cleardirty() { dirty = false; }
    public void markDirty()  { dirty = true; }

    public void generate() {
        int worldOffsetX = chunkX * CHUNK_SIZE;
        int worldOffsetZ = chunkZ * CHUNK_SIZE;

        int minTerrain = 5;
        int maxTerrain = depth - 3;

        for (int lz = 0; lz < CHUNK_SIZE; lz++) {
            for (int lx = 0; lx < CHUNK_SIZE; lx++) {
                double wx = worldOffsetX + lx;
                double wz = worldOffsetZ + lz;

                double biomeNoise = noise(wx * 0.0005 + 1000, wz * 0.0005 + 1000) + noise(wx * 0.001  + 1000, wz * 0.001  + 1000) * 0.5;
                biomeNoise /= 1.5;
                boolean isDesert = biomeNoise > 0.05;

                double continent = noise(wx * 0.003, wz * 0.003);
                double ridge = Math.abs(noise(wx * 0.015, wz * 0.015));
                double hills = noise(wx * 0.01,  wz * 0.01)  * 1.0
                             + noise(wx * 0.02,  wz * 0.02)  * 0.5
                             + noise(wx * 0.04,  wz * 0.04)  * 0.25
                             + noise(wx * 0.08,  wz * 0.08)  * 0.125;
                hills /= 1.875;

                double combined;
                if (isDesert) {
                    combined = continent * 0.55 + ridge * 0.1 + hills * 0.35;
                } else {
                    combined = continent * 0.45 + ridge * 0.3 + hills * 0.25;
                }

                int surfaceY = minTerrain + (int) ((combined + 0.6) / 1.4 * (maxTerrain - minTerrain));
                surfaceY = Math.max(minTerrain, Math.min(maxTerrain, surfaceY));

                for (int y = 0; y < depth; y++) {
                    byte id;

                    if (y == 0) {
                        id = 2; // cobblestone bedrock
                    } else if (y > surfaceY) {
                        id = 0; // air
                    } else if (isDesert) {
                        // Desert biome: sand top layers, cobble deep
                        if (y >= surfaceY - 5) {
                            id = 5; // sand
                        } else {
                            id = 2; // cobblestone
                        }
                    } else {
                        // Grassy hills biome
                        if (y == surfaceY) {
                            id = 1; // grass
                        } else if (y >= surfaceY - 3) {
                            id = 3; // dirt
                        } else {
                            id = 2; // cobblestone
                        }
                    }

                    blocks[index(lx, y, lz)] = id;
                }
            }
        }
        dirty = true;
    }

    private static final int[] PERM = new int[512];
    static {
        int[] p = {151,160,137,91,90,15,131,13,201,95,96,53,194,233,7,225,
                140,36,103,30,69,142,8,99,37,240,21,10,23,190,6,148,
                247,120,234,75,0,26,197,62,94,252,219,203,117,35,11,32,
                57,177,33,88,237,149,56,87,174,20,125,136,171,168,68,175,
                74,165,71,134,139,48,27,166,77,146,158,231,83,111,229,122,
                60,211,133,230,220,105,92,41,55,46,245,40,244,102,143,54,
                65,25,63,161,1,216,80,73,209,76,132,187,208,89,18,169,
                200,196,135,130,116,188,159,86,164,100,109,198,173,186,3,64,
                52,217,226,250,124,123,5,202,38,147,118,126,255,82,85,212,
                207,206,59,227,47,16,58,17,182,189,28,42,223,183,170,213,
                119,248,152,2,44,154,163,70,221,153,101,155,167,43,172,9,
                129,22,39,253,19,98,108,110,79,113,224,232,178,185,112,104,
                218,246,97,228,251,34,242,193,238,210,144,12,191,179,162,241,
                81,51,145,235,249,14,239,107,49,192,214,31,181,199,106,157,
                184,84,204,176,115,121,50,45,127,4,150,254,138,236,205,93,
                222,114,67,29,24,72,243,141,128,195,78,66,215,61,156,180};
        for (int i = 0; i < 256; i++) { PERM[i] = p[i]; PERM[i + 256] = p[i]; }
    }

    private static double noise(double x, double y) {
        int xi = (int) Math.floor(x) & 255;
        int yi = (int) Math.floor(y) & 255;
        double xf = x - Math.floor(x);
        double yf = y - Math.floor(y);
        double u = fade(xf);
        double v = fade(yf);
        int aa = PERM[PERM[xi]     + yi];
        int ab = PERM[PERM[xi]     + yi + 1];
        int ba = PERM[PERM[xi + 1] + yi];
        int bb = PERM[PERM[xi + 1] + yi + 1];
        return lerp(v, lerp(u, grad(aa, xf,     yf),
                               grad(ba, xf - 1, yf)),
                       lerp(u, grad(ab, xf,     yf - 1),
                               grad(bb, xf - 1, yf - 1)));
    }

    private static double fade(double t) { return t * t * t * (t * (t * 6 - 15) + 10); }
    private static double lerp(double t, double a, double b) { return a + t * (b - a); }
    private static double grad(int hash, double x, double y) {
        int h = hash & 3;
        double u = h < 2 ? x : y;
        double v = h < 2 ? y : x;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }

    private static Path chunkPath(Path dir, int cx, int cz) {
        return dir.resolve("c_" + cx + "_" + cz + ".dat");
    }

    public boolean load(Path chunkDir) {
        Path p = chunkPath(chunkDir, chunkX, chunkZ);
        if (!Files.exists(p)) return false;
        try (DataInputStream dis = new DataInputStream(new GZIPInputStream(Files.newInputStream(p)))) {
            dis.readFully(blocks);
            dirty = false;
            return true;
        } catch (IOException e) {
            System.err.println("Failed to load " + p + ": " + e.getMessage());
            return false;
        }
    }

    public void save(Path chunkDir) {
        if (!dirty) return;
        Path p = chunkPath(chunkDir, chunkX, chunkZ);
        try {
            Files.createDirectories(chunkDir);
            try (DataOutputStream dos = new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(p)))) {
                dos.write(blocks);
            }
            dirty = false;
        } catch (IOException e) {
            System.err.println("Failed to save " + p + ": " + e.getMessage());
        }
    }

    public void forceSave(Path chunkDir) {
        markDirty();
        save(chunkDir);
    }
}