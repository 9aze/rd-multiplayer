package client.level;

public class Tile {

    public static Tile grass = new Tile(0);
    public static Tile rock = new Tile(1);

    private final int textureId;

    public Tile(int textureId) {
        this.textureId = textureId;
    }

    public void render(Tessellator tessellator, Level level, int layer, int x, int y, int z) {
        float minU = this.textureId / 16.0F;
        float maxU = minU + 16 / 256F;
        float minV = 0.0F;
        float maxV = minV + 16 / 256F;

        float shadeX = 0.6f;
        float shadeY = 1.0f;
        float shadeZ = 0.8f;

        float minX = x + 0.0F;
        float maxX = x + 1.0F;
        float minY = y + 0.0F;
        float maxY = y + 1.0F;
        float minZ = z + 0.0F;
        float maxZ = z + 1.0F;

        if (!level.isSolidTile(x, y - 1, z)) {
            float brightness = level.getBrightness(x, y - 1, z) * shadeY;

            if (layer == 1 ^ brightness == shadeY) {
                tessellator.color(brightness, brightness, brightness);
                tessellator.texture(minU, maxV);
                tessellator.vertex(minX, minY, maxZ);
                tessellator.texture(minU, minV);
                tessellator.vertex(minX, minY, minZ);
                tessellator.texture(maxU, minV);
                tessellator.vertex(maxX, minY, minZ);
                tessellator.texture(maxU, maxV);
                tessellator.vertex(maxX, minY, maxZ);
            }
        }

        if (!level.isSolidTile(x, y + 1, z)) {
            float brightness = level.getBrightness(x, y + 1, z) * shadeY;

            if (layer == 1 ^ brightness == shadeY) {
                tessellator.color(brightness, brightness, brightness);
                tessellator.texture(maxU, maxV);
                tessellator.vertex(maxX, maxY, maxZ);
                tessellator.texture(maxU, minV);
                tessellator.vertex(maxX, maxY, minZ);
                tessellator.texture(minU, minV);
                tessellator.vertex(minX, maxY, minZ);
                tessellator.texture(minU, maxV);
                tessellator.vertex(minX, maxY, maxZ);
            }
        }

        if (!level.isSolidTile(x, y, z - 1)) {
            float brightness = level.getBrightness(x, y, z - 1) * shadeZ;

            if (layer == 1 ^ brightness == shadeZ) {
                tessellator.color(brightness, brightness, brightness);
                tessellator.texture(maxU, minV);
                tessellator.vertex(minX, maxY, minZ);
                tessellator.texture(minU, minV);
                tessellator.vertex(maxX, maxY, minZ);
                tessellator.texture(minU, maxV);
                tessellator.vertex(maxX, minY, minZ);
                tessellator.texture(maxU, maxV);
                tessellator.vertex(minX, minY, minZ);
            }
        }
        if (!level.isSolidTile(x, y, z + 1)) {
            float brightness = level.getBrightness(x, y, z + 1) * shadeZ;

            if (layer == 1 ^ brightness == shadeZ) {
                tessellator.color(brightness, brightness, brightness);
                tessellator.texture(minU, minV);
                tessellator.vertex(minX, maxY, maxZ);
                tessellator.texture(minU, maxV);
                tessellator.vertex(minX, minY, maxZ);
                tessellator.texture(maxU, maxV);
                tessellator.vertex(maxX, minY, maxZ);
                tessellator.texture(maxU, minV);
                tessellator.vertex(maxX, maxY, maxZ);
            }
        }

        if (!level.isSolidTile(x - 1, y, z)) {
            float brightness = level.getBrightness(x - 1, y, z) * shadeX;

            if (layer == 1 ^ brightness == shadeX) {
                tessellator.color(brightness, brightness, brightness);
                tessellator.texture(maxU, minV);
                tessellator.vertex(minX, maxY, maxZ);
                tessellator.texture(minU, minV);
                tessellator.vertex(minX, maxY, minZ);
                tessellator.texture(minU, maxV);
                tessellator.vertex(minX, minY, minZ);
                tessellator.texture(maxU, maxV);
                tessellator.vertex(minX, minY, maxZ);
            }
        }
        if (!level.isSolidTile(x + 1, y, z)) {
            float brightness = level.getBrightness(x + 1, y, z) * shadeX;

            if (layer == 1 ^ brightness == shadeX) {
                tessellator.color(brightness, brightness, brightness);
                tessellator.texture(minU, maxV);
                tessellator.vertex(maxX, minY, maxZ);
                tessellator.texture(maxU, maxV);
                tessellator.vertex(maxX, minY, minZ);
                tessellator.texture(maxU, minV);
                tessellator.vertex(maxX, maxY, minZ);
                tessellator.texture(minU, minV);
                tessellator.vertex(maxX, maxY, maxZ);
            }
        }
    }

    public void renderFace(Tessellator tessellator, int x, int y, int z, int face) {
        float minX = x + 0.0f;
        float maxX = x + 1.0f;
        float minY = y + 0.0f;
        float maxY = y + 1.0f;
        float minZ = z + 0.0f;
        float maxZ = z + 1.0f;

        if (face == 0) {
            tessellator.vertex(minX, minY, maxZ);
            tessellator.vertex(minX, minY, minZ);
            tessellator.vertex(maxX, minY, minZ);
            tessellator.vertex(maxX, minY, maxZ);
        }
        if (face == 1) {
            tessellator.vertex(maxX, maxY, maxZ);
            tessellator.vertex(maxX, maxY, minZ);
            tessellator.vertex(minX, maxY, minZ);
            tessellator.vertex(minX, maxY, maxZ);
        }
        if (face == 2) {
            tessellator.vertex(minX, maxY, minZ);
            tessellator.vertex(maxX, maxY, minZ);
            tessellator.vertex(maxX, minY, minZ);
            tessellator.vertex(minX, minY, minZ);
        }
        if (face == 3) {
            tessellator.vertex(minX, maxY, maxZ);
            tessellator.vertex(minX, minY, maxZ);
            tessellator.vertex(maxX, minY, maxZ);
            tessellator.vertex(maxX, maxY, maxZ);
        }
        if (face == 4) {
            tessellator.vertex(minX, maxY, maxZ);
            tessellator.vertex(minX, maxY, minZ);
            tessellator.vertex(minX, minY, minZ);
            tessellator.vertex(minX, minY, maxZ);
        }
        if (face == 5) {
            tessellator.vertex(maxX, minY, maxZ);
            tessellator.vertex(maxX, minY, minZ);
            tessellator.vertex(maxX, maxY, minZ);
            tessellator.vertex(maxX, maxY, maxZ);
        }
    }
}