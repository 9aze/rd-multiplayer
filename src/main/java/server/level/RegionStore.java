package server.level;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RegionStore {

    private static final int CACHE_LIMIT = 64;
    private final Path chunkDir;

    private final LinkedHashMap<Long, RegionFile> regions =
            new LinkedHashMap<Long, RegionFile>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Long, RegionFile> eldest) {
                    if (size() <= CACHE_LIMIT) return false;
                    eldest.getValue().close();
                    return true;
                }
            };

    public RegionStore(Path chunkDir) {
        this.chunkDir = chunkDir;
    }

    private static long regionKey(int rx, int ry, int rz) {
        return ((long)(rx & 0x1FFFFF) << 42) | ((long)(ry & 0x1FFFFF) << 21) |  (long)(rz & 0x1FFFFF);
    }

    private Path regionPath(int rx, int ry, int rz) {
        return chunkDir.resolve("r." + rx + "." + ry + "." + rz + ".mcr");
    }

    private Path legacyPath(int cx, int cy, int cz) {
        return chunkDir.resolve("c_" + cx + "_" + cy + "_" + cz + ".dat");
    }

    private synchronized RegionFile region(int rx, int ry, int rz) throws IOException {
        long key = regionKey(rx, ry, rz);
        RegionFile rf = regions.get(key);
        if (rf == null) {
            rf = new RegionFile(regionPath(rx, ry, rz));
            regions.put(key, rf);
        }
        return rf;
    }

    public byte[] read(int cx, int cy, int cz) {
        int rx = cx >> RegionFile.REGION_BITS;
        int ry = cy >> RegionFile.REGION_BITS;
        int rz = cz >> RegionFile.REGION_BITS;
        int lx = cx & (RegionFile.REGION_SIZE - 1);
        int ly = cy & (RegionFile.REGION_SIZE - 1);
        int lz = cz & (RegionFile.REGION_SIZE - 1);

        try {
            RegionFile rf;
            synchronized (this) {
                rf = regions.get(regionKey(rx, ry, rz));
            }
            if (rf == null && !Files.exists(regionPath(rx, ry, rz))) {
                Path legacy = legacyPath(cx, cy, cz);
                if (Files.exists(legacy)) {
                    byte[] migrated = readLegacy(legacy);
                    if (migrated != null) {
                        RegionFile newRf = region(rx, ry, rz);
                        newRf.write(lx, ly, lz, migrated);
                        try { Files.delete(legacy); } catch (IOException ignored) {}
                        return migrated;
                    }
                }
                return null;
            }

            rf = region(rx, ry, rz);
            byte[] data = rf.read(lx, ly, lz);
            if (data != null) return data;

            // region exists
            Path legacy = legacyPath(cx, cy, cz);
            if (Files.exists(legacy)) {
                byte[] migrated = readLegacy(legacy);
                if (migrated != null) {
                    rf.write(lx, ly, lz, migrated);
                    try { Files.delete(legacy); } catch (IOException ignored) {}
                    return migrated;
                }
            }
            return null;
        } catch (IOException e) {
            System.err.println("RegionStore.read failed for (" + cx + ", " + cy + ", " + cz + "): " + e.getMessage());
            return null;
        }
    }

    /** read and update a legacy chunk */
    private byte[] readLegacy(Path legacy) {
        try (java.io.DataInputStream dis = new java.io.DataInputStream(
                new java.util.zip.GZIPInputStream(Files.newInputStream(legacy)))) {
            byte[] data = new byte[LevelChunk.VOLUME];
            dis.readFully(data);
            return data;
        } catch (IOException e) {
            System.err.println("Failed to migrate legacy chunk " + legacy + ": " + e.getMessage());
            return null;
        }
    }

    /** Write a chunk's bytes to disk */
    public void write(int cx, int cy, int cz, byte[] data) {
        int rx = cx >> RegionFile.REGION_BITS;
        int ry = cy >> RegionFile.REGION_BITS;
        int rz = cz >> RegionFile.REGION_BITS;
        int lx = cx & (RegionFile.REGION_SIZE - 1);
        int ly = cy & (RegionFile.REGION_SIZE - 1);
        int lz = cz & (RegionFile.REGION_SIZE - 1);
        try {
            region(rx, ry, rz).write(lx, ly, lz, data);
        } catch (IOException e) {
            System.err.println("RegionStore.write failed for (" + cx + ", " + cy + ", " + cz + "): " + e.getMessage());
        }
    }

    /** close region files */
    public synchronized void closeAll() {
        for (RegionFile rf : regions.values()) rf.close();
        regions.clear();
    }
}