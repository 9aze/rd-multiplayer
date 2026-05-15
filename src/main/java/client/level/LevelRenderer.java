package client.level;

import client.*;
import client.phys.AABB;
import client.net.PlayerManager;
import org.lwjgl.BufferUtils;

import java.util.Map;

import static org.lwjgl.opengl.GL11.*;

public class LevelRenderer implements LevelListener {

    private static final int CHUNK_SIZE = 16;

    private final Tessellator tessellator;
    private final Level level;
    private final Chunk[] chunks;

    private final int chunkAmountX;
    private final int chunkAmountY;
    private final int chunkAmountZ;

    /**
     * Create renderer for level
     *
     * @param level The rendered level
     */
    public LevelRenderer(Level level) {
        level.addListener(this);

        this.tessellator = new Tessellator();
        this.level = level;

        // Calculate amount of chunks of level
        this.chunkAmountX = level.width / CHUNK_SIZE;
        this.chunkAmountY = level.depth / CHUNK_SIZE;
        this.chunkAmountZ = level.height / CHUNK_SIZE;

        // Create chunk array
        this.chunks = new Chunk[this.chunkAmountX * this.chunkAmountY * this.chunkAmountZ];

        // Fill level with chunks
        for (int x = 0; x < this.chunkAmountX; x++) {
            for (int y = 0; y < this.chunkAmountY; y++) {
                for (int z = 0; z < this.chunkAmountZ; z++) {
                    // Calculate min bounds for chunk
                    int minChunkX = x * CHUNK_SIZE;
                    int minChunkY = y * CHUNK_SIZE;
                    int minChunkZ = z * CHUNK_SIZE;

                    // Calculate max bounds for chunk
                    int maxChunkX = (x + 1) * CHUNK_SIZE;
                    int maxChunkY = (y + 1) * CHUNK_SIZE;
                    int maxChunkZ = (z + 1) * CHUNK_SIZE;

                    // Check for chunk bounds out of level
                    maxChunkX = Math.min(level.width, maxChunkX);
                    maxChunkY = Math.min(level.depth, maxChunkY);
                    maxChunkZ = Math.min(level.height, maxChunkZ);

                    // Create chunk based on bounds
                    Chunk chunk = new Chunk(level, minChunkX, minChunkY, minChunkZ, maxChunkX, maxChunkY, maxChunkZ);
                    this.chunks[(x + y * this.chunkAmountX) * this.chunkAmountZ + z] = chunk;
                }
            }
        }
    }

    /**
     * Render all chunks of the level
     *
     * @param layer The render layer
     */
    public void render(int layer) {
        // Get current camera frustum
        Frustum frustum = Frustum.getFrustum();

        // Reset global chunk rebuild stats
        Chunk.rebuiltThisFrame = 0;

        // For all chunks
        for (Chunk chunk : this.chunks) {

            // Render if bounding box of chunk is in frustum
            if (frustum.cubeInFrustum(chunk.boundingBox)) {

                // Render chunk
                chunk.render(layer);
            }
        }
    }

    /**
     * Mark all chunks inside of the given area as dirty.
     *
     * @param minX Minimum on X axis
     * @param minY Minimum on Y axis
     * @param minZ Minimum on Z axis
     * @param maxX Maximum on X axis
     * @param maxY Maximum on Y axis
     * @param maxZ Maximum on Z axis
     */
    public void setDirty(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        // To chunk coordinates
        minX /= CHUNK_SIZE;
        minY /= CHUNK_SIZE;
        minZ /= CHUNK_SIZE;
        maxX /= CHUNK_SIZE;
        maxY /= CHUNK_SIZE;
        maxZ /= CHUNK_SIZE;

        // Minimum limit
        minX = Math.max(minX, 0);
        minY = Math.max(minY, 0);
        minZ = Math.max(minZ, 0);

        // Maximum limit
        maxX = Math.min(maxX, this.chunkAmountX - 1);
        maxY = Math.min(maxY, this.chunkAmountY - 1);
        maxZ = Math.min(maxZ, this.chunkAmountZ - 1);

        // Mark all chunks as dirty
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    // Get chunk at this position
                    Chunk chunk = this.chunks[(x + y * this.chunkAmountX) * this.chunkAmountZ + z];

                    // Set dirty
                    chunk.setDirty();
                }
            }
        }
    }

    /**
     * Render pick selection face on tile
     *
     * @param player The player
     */
    public void pick(Player player) {
        float radius = 3.0F;
        AABB boundingBox = player.boundingBox.grow(radius, radius, radius);

        int minX = (int) boundingBox.minX;
        int maxX = (int) (boundingBox.maxX + 1.0f);
        int minY = (int) boundingBox.minY;
        int maxY = (int) (boundingBox.maxY + 1.0f);
        int minZ = (int) boundingBox.minZ;
        int maxZ = (int) (boundingBox.maxZ + 1.0f);

        glInitNames();
        for (int x = minX; x < maxX; x++) {
            // Name value x
            glPushName(x);
            for (int y = minY; y < maxY; y++) {
                // Name value y
                glPushName(y);
                for (int z = minZ; z < maxZ; z++) {
                    // Name value z
                    glPushName(z);

                    // Check for solid tile
                    if (this.level.isSolidTile(x, y, z)) {

                        // Name value type
                        glPushName(0);

                        // Render all faces
                        for (int face = 0; face < 6; face++) {

                            // Name value face id
                            glPushName(face);

                            // Render selection face
                            this.tessellator.init();
                            Tile.rock.renderFace(this.tessellator, x, y, z, face);
                            this.tessellator.flush();

                            glPopName();
                        }
                        glPopName();
                    }
                    glPopName();
                }
                glPopName();
            }
            glPopName();
        }
    }

    /**
     * Render hit face of the result
     *
     * @param hitResult The hit result to render
     */
    public void renderHit(HitResult hitResult) {
        // Setup blending and color
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_CURRENT_BIT);
        glColor4f(1.0f, 1.0f, 1.0f, (float) Math.sin(System.currentTimeMillis() / 100.0) * 0.2f + 0.4f);

        // Render face
        this.tessellator.init();
        Tile.rock.renderFace(this.tessellator, hitResult.x, hitResult.y, hitResult.z, hitResult.face);
        this.tessellator.flush();

        // Disable blending
        glDisable(GL_BLEND);
    }

    public void renderPlayers(PlayerManager playerManager) {
        glDisable(GL_TEXTURE_2D);
        glDisable(GL_CULL_FACE);
        glDisable(GL_FOG);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        for (Map.Entry<String, Position> entry : playerManager.getPlayers().entrySet()) {
            Position pos = entry.getValue();
            glPushMatrix();
            glTranslatef((float) pos.x, (float) pos.y - 1.62f, (float) pos.z);
            glRotatef(-pos.yaw, 0f, 1f, 0f);
            this.tessellator.init();
            renderPlayerModel();
            this.tessellator.flush();
            glPopMatrix();
        }

        glDisable(GL_BLEND);
        glEnable(GL_CULL_FACE);
        glEnable(GL_FOG);
        glEnable(GL_TEXTURE_2D);
    }

    private void renderPlayerModel() {
        renderBox(-0.25f, 1.25f, -0.25f,  0.25f, 1.75f,  0.25f,  0.85f, 0.65f, 0.50f);
        renderBox(-0.25f, 0.50f, -0.125f, 0.25f, 1.25f,  0.125f, 0.22f, 0.40f, 0.75f);
        renderBox( 0.00f, 0.00f, -0.125f, 0.25f, 0.50f,  0.125f, 0.15f, 0.25f, 0.55f);
        renderBox(-0.25f, 0.00f, -0.125f, 0.00f, 0.50f,  0.125f, 0.15f, 0.25f, 0.55f);
        renderBox( 0.25f, 0.50f, -0.125f, 0.50f, 1.25f,  0.125f, 0.85f, 0.65f, 0.50f);
        renderBox(-0.50f, 0.50f, -0.125f,-0.25f, 1.25f,  0.125f, 0.85f, 0.65f, 0.50f);
    }

    private void renderBox(float x0, float y0, float z0, float x1, float y1, float z1,
                           float r, float g, float b) {
        this.tessellator.color(r, g, b);

        this.tessellator.vertex(x0, y0, z0);
        this.tessellator.vertex(x1, y0, z0);
        this.tessellator.vertex(x1, y0, z1);
        this.tessellator.vertex(x0, y0, z1);

        this.tessellator.vertex(x0, y1, z0);
        this.tessellator.vertex(x1, y1, z0);
        this.tessellator.vertex(x1, y1, z1);
        this.tessellator.vertex(x0, y1, z1);

        this.tessellator.vertex(x0, y0, z0);
        this.tessellator.vertex(x1, y0, z0);
        this.tessellator.vertex(x1, y1, z0);
        this.tessellator.vertex(x0, y1, z0);

        this.tessellator.vertex(x0, y0, z1);
        this.tessellator.vertex(x1, y0, z1);
        this.tessellator.vertex(x1, y1, z1);
        this.tessellator.vertex(x0, y1, z1);

        this.tessellator.vertex(x0, y0, z0);
        this.tessellator.vertex(x0, y1, z0);
        this.tessellator.vertex(x0, y1, z1);
        this.tessellator.vertex(x0, y0, z1);

        this.tessellator.vertex(x1, y0, z0);
        this.tessellator.vertex(x1, y1, z0);
        this.tessellator.vertex(x1, y1, z1);
        this.tessellator.vertex(x1, y0, z1);
    }

    @Override
    public void lightColumnChanged(int x, int z, int minY, int maxY) {
        setDirty(x - 1, minY - 1, z - 1, x + 1, maxY + 1, z + 1);
    }

    @Override
    public void tileChanged(int x, int y, int z) {
        setDirty(x - 1, y - 1, z - 1, x + 1, y + 1, z + 1);
    }

    @Override
    public void allChanged() {
        setDirty(0, 0, 0, this.level.width, this.level.depth, this.level.height);
    }
}
