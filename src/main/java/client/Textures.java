package client;

import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.util.glu.GLU.gluBuild2DMipmaps;

public class Textures {

    public static int loadTexture(String resourceName, int mode) {
        int id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, mode);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, mode);

        InputStream inputStream = Textures.class.getResourceAsStream(resourceName);

        try {
            BufferedImage bufferedImage = ImageIO.read(inputStream);

            int width  = bufferedImage.getWidth();
            int height = bufferedImage.getHeight();

            int[] pixels = new int[width * height];
            bufferedImage.getRGB(0, 0, width, height, pixels, 0, width);

            for (int i = 0; i < pixels.length; i++) {
                int alpha = pixels[i] >> 24 & 0xFF;
                int red   = pixels[i] >> 16 & 0xFF;
                int green = pixels[i] >>  8 & 0xFF;
                int blue  = pixels[i]       & 0xFF;
                pixels[i] = alpha << 24 | blue << 16 | green << 8 | red;
            }

            ByteBuffer byteBuffer = BufferUtils.createByteBuffer(width * height * 4);
            byteBuffer.asIntBuffer().put(pixels);

            gluBuild2DMipmaps(GL_TEXTURE_2D, GL_RGBA, width, height, GL_RGBA, GL_UNSIGNED_BYTE, byteBuffer);
        } catch (IOException exception) {
            throw new RuntimeException("Could not load texture " + resourceName, exception);
        }

        return id;
    }

    public static void bind(int id) {
        glBindTexture(GL_TEXTURE_2D, id);
    }
}