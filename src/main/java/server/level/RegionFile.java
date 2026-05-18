package server.level;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;

final class RegionFile {

    static final int REGION_BITS = 5; // 32 chunks per axis
    static final int REGION_SIZE = 1 << REGION_BITS;  // 32
    static final int CHUNKS_PER_REGION = REGION_SIZE * REGION_SIZE * REGION_SIZE;
    static final int CHUNK_BYTES = LevelChunk.VOLUME; // 4096

    static final int SECTOR_BYTES = 4096;
    static final int HEADER_BYTES = CHUNKS_PER_REGION * 8;
    static final int HEADER_SECTORS = HEADER_BYTES / SECTOR_BYTES; // 64

    private final Path path;
    private final RandomAccessFile raf;
    private final int[] sectorOffsets = new int[CHUNKS_PER_REGION];
    private final int[] byteLengths = new int[CHUNKS_PER_REGION];

    /** Bitmap of allocated sectors (header sectors are pre-allocated). */
    private final java.util.BitSet sectorUsed = new java.util.BitSet();

    RegionFile(Path path) throws IOException {
        this.path = path;
        Files.createDirectories(path.getParent());
        boolean isNew = !Files.exists(path);
        this.raf = new RandomAccessFile(path.toFile(), "rw");

        if (isNew) {
            // Pre write empty header.
            raf.setLength(HEADER_BYTES);
            // Header sectors marked allocated
            for (int i = 0; i < HEADER_SECTORS; i++) sectorUsed.set(i);
        } else {
            long len = raf.length();
            if (len < HEADER_BYTES) {
                // Corrupted shitfuck
                raf.setLength(HEADER_BYTES);
            }
            // Read header.
            raf.seek(0);
            for (int i = 0; i < CHUNKS_PER_REGION; i++) {
                int off = raf.readInt();
                int blen = raf.readInt();
                sectorOffsets[i] = off;
                byteLengths[i] = blen;
            }
            // Mark allocated sectors.
            for (int i = 0; i < HEADER_SECTORS; i++) sectorUsed.set(i);
            for (int i = 0; i < CHUNKS_PER_REGION; i++) {
                int off = sectorOffsets[i];
                if (off == 0) continue;
                int sectors = sectorsNeeded(byteLengths[i]);
                for (int s = 0; s < sectors; s++) sectorUsed.set(off + s);
            }
        }
    }

    private static int sectorsNeeded(int bytes) {
        return (bytes + SECTOR_BYTES - 1) / SECTOR_BYTES;
    }

    private static int slotIndex(int lx, int ly, int lz) {
        return (lx * REGION_SIZE + ly) * REGION_SIZE + lz;
    }

    synchronized byte[] read(int lx, int ly, int lz) throws IOException {
        int idx = slotIndex(lx, ly, lz);
        int off = sectorOffsets[idx];
        int blen = byteLengths[idx];
        if (off == 0 || blen <= 0) return null;
        raf.seek((long) off * SECTOR_BYTES);
        byte[] data = new byte[blen];
        raf.readFully(data);
        return data;
    }

    synchronized void write(int lx, int ly, int lz, byte[] data) throws IOException {
        if (data.length != CHUNK_BYTES) {
            throw new IllegalArgumentException(
                    "expected " + CHUNK_BYTES + " bytes, got " + data.length);
        }
        int idx = slotIndex(lx, ly, lz);
        int needed = sectorsNeeded(data.length);
        int existing = sectorOffsets[idx];

        if (existing != 0 && sectorsNeeded(byteLengths[idx]) >= needed) {
            raf.seek((long) existing * SECTOR_BYTES);
            raf.write(data);
            byteLengths[idx] = data.length;
            writeHeaderSlot(idx);
            return;
        }

        if (existing != 0) {
            int oldSectors = sectorsNeeded(byteLengths[idx]);
            for (int s = 0; s < oldSectors; s++) sectorUsed.clear(existing + s);
        }

        int newOffset = allocateSectors(needed);
        raf.seek((long) newOffset * SECTOR_BYTES);
        raf.write(data);
        sectorOffsets[idx] = newOffset;
        byteLengths[idx] = data.length;
        writeHeaderSlot(idx);

        long requiredLen = (long) (newOffset + needed) * SECTOR_BYTES;
        if (raf.length() < requiredLen) raf.setLength(requiredLen);
    }

    private int allocateSectors(int count) {
        int run = 0;
        int firstOfRun = -1;
        int limit = Math.max(sectorUsed.length(), HEADER_SECTORS);
        for (int i = HEADER_SECTORS; i <= limit; i++) {
            if (!sectorUsed.get(i)) {
                if (run == 0) firstOfRun = i;
                run++;
                if (run == count) {
                    for (int s = 0; s < count; s++) sectorUsed.set(firstOfRun + s);
                    return firstOfRun;
                }
            } else {
                run = 0;
                firstOfRun = -1;
            }
        }
        int start = Math.max(sectorUsed.length(), HEADER_SECTORS);
        for (int s = 0; s < count; s++) sectorUsed.set(start + s);
        return start;
    }

    private void writeHeaderSlot(int idx) throws IOException {
        raf.seek((long) idx * 8);
        raf.writeInt(sectorOffsets[idx]);
        raf.writeInt(byteLengths[idx]);
    }

    synchronized void close() {
        try {
            raf.getFD().sync();
        } catch (IOException ignored) {
        }
        try {
            raf.close();
        } catch (IOException e) {
            System.err.println("Failed to close region " + path + ": " + e.getMessage());
        }
    }
}