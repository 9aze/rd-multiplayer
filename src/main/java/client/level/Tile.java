package client.level;

public class Tile {
    public final int id;
    public final String name;
    public final FaceTextures faces;

    private static final float MIN_V = 0f;
    private static final float MAX_V = 16 / 256f;

    public Tile(int id, String name) {
        this(id, name, FaceTextures.uniform(id - 1));
    }

    public Tile(int id, String name, FaceTextures faces) {
        this.id = id;
        this.name = name;
        this.faces = faces;
    }

    private static float minU(int col) { return col / 16f; }
    private static float maxU(int col) { return col / 16f + 16 / 256f; }

    public void render(Tessellator t, Level level, int layer, int x, int y, int z) {

        float x0 = x, x1 = x + 1;
        float y0 = y, y1 = y + 1;
        float z0 = z, z1 = z + 1;

        // BOTTOM (face 0)
        {
            float u0 = minU(faces.col(FaceTextures.BOTTOM));
            float u1 = maxU(faces.col(FaceTextures.BOTTOM));
            face(t, level, layer, x, y - 1, z, 1f,
                    x0,y0,z1, x0,y0,z0, x1,y0,z0, x1,y0,z1,
                    u0,MAX_V, u0,MIN_V, u1,MIN_V, u1,MAX_V);
        }

        // TOP (face 1)
        {
            float u0 = minU(faces.col(FaceTextures.TOP));
            float u1 = maxU(faces.col(FaceTextures.TOP));
            face(t, level, layer, x, y + 1, z, 1f,
                    x1,y1,z1, x1,y1,z0, x0,y1,z0, x0,y1,z1,
                    u1,MAX_V, u1,MIN_V, u0,MIN_V, u0,MAX_V);
        }

        // NORTH (face 2, -Z)
        {
            float u0 = minU(faces.col(FaceTextures.NORTH));
            float u1 = maxU(faces.col(FaceTextures.NORTH));
            face(t, level, layer, x, y, z - 1, .8f,
                    x0,y1,z0, x1,y1,z0, x1,y0,z0, x0,y0,z0,
                    u1,MIN_V, u0,MIN_V, u0,MAX_V, u1,MAX_V);
        }

        // SOUTH (face 3, +Z)
        {
            float u0 = minU(faces.col(FaceTextures.SOUTH));
            float u1 = maxU(faces.col(FaceTextures.SOUTH));
            face(t, level, layer, x, y, z + 1, .8f,
                    x0,y1,z1, x0,y0,z1, x1,y0,z1, x1,y1,z1,
                    u0,MIN_V, u0,MAX_V, u1,MAX_V, u1,MIN_V);
        }

        // WEST (face 4, -X)
        {
            float u0 = minU(faces.col(FaceTextures.WEST));
            float u1 = maxU(faces.col(FaceTextures.WEST));
            face(t, level, layer, x - 1, y, z, .6f,
                    x0,y1,z1, x0,y1,z0, x0,y0,z0, x0,y0,z1,
                    u1,MIN_V, u0,MIN_V, u0,MAX_V, u1,MAX_V);
        }

        // EAST (face 5, +X)
        {
            float u0 = minU(faces.col(FaceTextures.EAST));
            float u1 = maxU(faces.col(FaceTextures.EAST));
            face(t, level, layer, x + 1, y, z, .6f,
                    x1,y0,z1, x1,y0,z0, x1,y1,z0, x1,y1,z1,
                    u0,MAX_V, u1,MAX_V, u1,MIN_V, u0,MIN_V);
        }
    }

    private void face(
            Tessellator t, Level level, int layer,
            int x, int y, int z,
            float shade,

            float x0,float y0,float z0,
            float x1,float y1,float z1,
            float x2,float y2,float z2,
            float x3,float y3,float z3,

            float u0,float v0,
            float u1,float v1,
            float u2,float v2,
            float u3,float v3
    ) {

        if (level.isSolidTile(x, y, z)) return;

        float b = level.getBrightness(x, y, z) * shade;

        if (!(layer == 1 ^ b == shade)) return;

        t.color(b, b, b);

        t.texture(u0, v0); t.vertex(x0, y0, z0);
        t.texture(u1, v1); t.vertex(x1, y1, z1);
        t.texture(u2, v2); t.vertex(x2, y2, z2);
        t.texture(u3, v3); t.vertex(x3, y3, z3);
    }

    public void renderFace(Tessellator t, int x, int y, int z, int face) {

        float x0 = x, x1 = x + 1;
        float y0 = y, y1 = y + 1;
        float z0 = z, z1 = z + 1;

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