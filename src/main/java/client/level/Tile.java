package client.level;

public class Tile {

    public static final Tile AIR      = null;
    public static final Tile GRASS    = new Tile(0, "Grass");       // col 0
    public static final Tile COBBLE   = new Tile(1, "Cobblestone"); // col 1
    public static final Tile DIRT     = new Tile(2, "Dirt");        // col 2
    public static final Tile OBSIDIAN = new Tile(3, "Obsidian");    // col 3
    public static final Tile SAND     = new Tile(4, "Sand");        // col 4
    public static final Tile BRICKS   = new Tile(5, "Bricks");      // col 5

    public static final Tile[] BY_ID = {
            AIR,      // 0
            GRASS,    // 1
            COBBLE,   // 2
            DIRT,     // 3
            OBSIDIAN, // 4
            SAND,     // 5
            BRICKS    // 6
    };

    private final int atlasCol;
    public  final String name;

    public Tile(int atlasCol, String name) {
        this.atlasCol = atlasCol;
        this.name     = name;
    }

    public Tile(int atlasCol) { this(atlasCol, "Block"); }

    public static Tile fromId(int id) {
        if (id <= 0 || id >= BY_ID.length) return null;
        return BY_ID[id];
    }

    private float minU() { return atlasCol / 16.0F; }
    private float maxU() { return minU() + 16 / 256.0F; }
    private static final float MIN_V = 0.0F;
    private static final float MAX_V = 16 / 256.0F;

    public void render(Tessellator t, Level level, int layer, int x, int y, int z) {
        float minU = minU(), maxU = maxU();
        float shadeX = 0.6f, shadeY = 1.0f, shadeZ = 0.8f;

        float x0 = x, x1 = x+1, y0 = y, y1 = y+1, z0 = z, z1 = z+1;

        // bottom
        if (!level.isSolidTile(x, y-1, z)) {
            float b = level.getBrightness(x, y-1, z) * shadeY;
            if (layer == 1 ^ b == shadeY) {
                t.color(b,b,b);
                t.texture(minU, MAX_V); t.vertex(x0, y0, z1);
                t.texture(minU, MIN_V); t.vertex(x0, y0, z0);
                t.texture(maxU, MIN_V); t.vertex(x1, y0, z0);
                t.texture(maxU, MAX_V); t.vertex(x1, y0, z1);
            }
        }
        // top
        if (!level.isSolidTile(x, y+1, z)) {
            float b = level.getBrightness(x, y+1, z) * shadeY;
            if (layer == 1 ^ b == shadeY) {
                t.color(b,b,b);
                t.texture(maxU, MAX_V); t.vertex(x1, y1, z1);
                t.texture(maxU, MIN_V); t.vertex(x1, y1, z0);
                t.texture(minU, MIN_V); t.vertex(x0, y1, z0);
                t.texture(minU, MAX_V); t.vertex(x0, y1, z1);
            }
        }
        // north (z-)
        if (!level.isSolidTile(x, y, z-1)) {
            float b = level.getBrightness(x, y, z-1) * shadeZ;
            if (layer == 1 ^ b == shadeZ) {
                t.color(b,b,b);
                t.texture(maxU, MIN_V); t.vertex(x0, y1, z0);
                t.texture(minU, MIN_V); t.vertex(x1, y1, z0);
                t.texture(minU, MAX_V); t.vertex(x1, y0, z0);
                t.texture(maxU, MAX_V); t.vertex(x0, y0, z0);
            }
        }
        // south (z+)
        if (!level.isSolidTile(x, y, z+1)) {
            float b = level.getBrightness(x, y, z+1) * shadeZ;
            if (layer == 1 ^ b == shadeZ) {
                t.color(b,b,b);
                t.texture(minU, MIN_V); t.vertex(x0, y1, z1);
                t.texture(minU, MAX_V); t.vertex(x0, y0, z1);
                t.texture(maxU, MAX_V); t.vertex(x1, y0, z1);
                t.texture(maxU, MIN_V); t.vertex(x1, y1, z1);
            }
        }
        // west (x-)
        if (!level.isSolidTile(x-1, y, z)) {
            float b = level.getBrightness(x-1, y, z) * shadeX;
            if (layer == 1 ^ b == shadeX) {
                t.color(b,b,b);
                t.texture(maxU, MIN_V); t.vertex(x0, y1, z1);
                t.texture(minU, MIN_V); t.vertex(x0, y1, z0);
                t.texture(minU, MAX_V); t.vertex(x0, y0, z0);
                t.texture(maxU, MAX_V); t.vertex(x0, y0, z1);
            }
        }
        // east (x+)
        if (!level.isSolidTile(x+1, y, z)) {
            float b = level.getBrightness(x+1, y, z) * shadeX;
            if (layer == 1 ^ b == shadeX) {
                t.color(b,b,b);
                t.texture(minU, MAX_V); t.vertex(x1, y0, z1);
                t.texture(maxU, MAX_V); t.vertex(x1, y0, z0);
                t.texture(maxU, MIN_V); t.vertex(x1, y1, z0);
                t.texture(minU, MIN_V); t.vertex(x1, y1, z1);
            }
        }
    }

    public void renderFace(Tessellator t, int x, int y, int z, int face) {
        float x0=x, x1=x+1, y0=y, y1=y+1, z0=z, z1=z+1;
        switch (face) {
            case 0: t.vertex(x0,y0,z1); t.vertex(x0,y0,z0); t.vertex(x1,y0,z0); t.vertex(x1,y0,z1); break;
            case 1: t.vertex(x1,y1,z1); t.vertex(x1,y1,z0); t.vertex(x0,y1,z0); t.vertex(x0,y1,z1); break;
            case 2: t.vertex(x0,y1,z0); t.vertex(x1,y1,z0); t.vertex(x1,y0,z0); t.vertex(x0,y0,z0); break;
            case 3: t.vertex(x0,y1,z1); t.vertex(x0,y0,z1); t.vertex(x1,y0,z1); t.vertex(x1,y1,z1); break;
            case 4: t.vertex(x0,y1,z1); t.vertex(x0,y1,z0); t.vertex(x0,y0,z0); t.vertex(x0,y0,z1); break;
            case 5: t.vertex(x1,y0,z1); t.vertex(x1,y0,z0); t.vertex(x1,y1,z0); t.vertex(x1,y1,z1); break;
        }
    }
}