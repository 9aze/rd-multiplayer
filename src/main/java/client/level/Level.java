package client.level;

import client.phys.AABB;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Level {

    public int width;
    public int height;
    public int depth;

    private byte[] blocks;
    private int[] lightDepths;

    private final ArrayList<LevelListener> levelListeners = new ArrayList<>();

    public Level(int width, int height, int depth) {
        this.width = width;
        this.height = height;
        this.depth = depth;

        this.blocks = new byte[width * height * depth];
        this.lightDepths = new int[width * height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < depth; y++) {
                for (int z = 0; z < height; z++) {
                    int index = (y * this.height + z) * this.width + x;

                    this.blocks[index] = (byte) ((y <= depth * 2 / 3) ? 1 : 0);
                }
            }
        }

        calcLightDepths(0, 0, width, height);

    }

    public void loadLevel(int w, int h, int d, byte[] blocks) {
        this.width = w;
        this.height = h;
        this.depth = d;
        this.blocks = blocks;

        this.lightDepths = new int[w * h];

        calcLightDepths(0, 0, w, h);
        for (LevelListener levelListener : this.levelListeners) {
            levelListener.allChanged();
        }
    }

    private void calcLightDepths(int minX, int minZ, int maxX, int maxZ) {
        for (int x = minX; x < minX + maxX; x++) {
            for (int z = minZ; z < minZ + maxZ; z++) {

                int prevDepth = this.lightDepths[x + z * this.width];

                int depth = this.depth - 1;
                while (depth > 0 && !isLightBlocker(x, depth, z)) {
                    depth--;
                }

                this.lightDepths[x + z * this.width] = depth;

                if (prevDepth != depth) {
                    int minTileChangeY = Math.min(prevDepth, depth);
                    int maxTileChangeY = Math.max(prevDepth, depth);

                    for (LevelListener levelListener : this.levelListeners) {
                        levelListener.lightColumnChanged(x, z, minTileChangeY, maxTileChangeY);
                    }
                }
            }
        }
    }

    public boolean isTile(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= this.width || y >= this.depth || z >= this.height) {
            return false;
        }

        int index = (y * this.height + z) * this.width + x;

        return this.blocks[index] != 0;
    }

    public boolean isSolidTile(int x, int y, int z) {
        return isTile(x, y, z);
    }

    public boolean isLightBlocker(final int x, final int y, final int z) {
        return this.isSolidTile(x, y, z);
    }

    public float getBrightness(int x, int y, int z) {
        float dark = 0.8F;
        float light = 1.0F;

        if (x < 0 || y < 0 || z < 0 || x >= this.width || y >= this.depth || z >= this.height) {
            return light;
        }

        if (y < this.lightDepths[x + z * this.width]) {
            return dark;
        }

        return light;
    }


    public ArrayList<AABB> getCubes(AABB boundingBox) {
        ArrayList<AABB> boundingBoxList = new ArrayList<>();

        int minX = (int) (Math.floor(boundingBox.minX) - 1);
        int maxX = (int) (Math.ceil(boundingBox.maxX) + 1);
        int minY = (int) (Math.floor(boundingBox.minY) - 1);
        int maxY = (int) (Math.ceil(boundingBox.maxY) + 1);
        int minZ = (int) (Math.floor(boundingBox.minZ) - 1);
        int maxZ = (int) (Math.ceil(boundingBox.maxZ) + 1);

        minX = Math.max(0, minX);
        minY = Math.max(0, minY);
        minZ = Math.max(0, minZ);

        maxX = Math.min(this.width, maxX);
        maxY = Math.min(this.depth, maxY);
        maxZ = Math.min(this.height, maxZ);

        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    if (isSolidTile(x, y, z)) {
                        boundingBoxList.add(new AABB(x, y, z, x + 1, y + 1, z + 1));
                    }
                }
            }
        }
        return boundingBoxList;
    }


    public void setTile(int x, int y, int z, int id) {
        if (x < 0 || y < 0 || z < 0 || x >= this.width || y >= this.depth || z >= this.height) {
            return;
        }

        this.blocks[(y * this.height + z) * this.width + x] = (byte) id;

        this.calcLightDepths(x, z, 1, 1);

        for (LevelListener levelListener : this.levelListeners) {
            levelListener.tileChanged(x, y, z);
        }
    }

    public void addListener(LevelListener levelListener) {
        this.levelListeners.add(levelListener);
    }

    public byte[] getBlocks() {return this.blocks;}

    public int getWidth() { return this.width; }
    public int getHeight() { return this.height; }
    public int getDepth() { return this.depth; }
}