package com.replayrec.recording;

import com.replayrec.ReplayRecMod;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class FrameCapture {
    private int width;
    private int height;
    private ByteBuffer pixelBuffer;

    public FrameCapture(int width, int height) {
        this.width = width;
        this.height = height;
        this.pixelBuffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder());
    }

    public BufferedImage captureFrame() {
        pixelBuffer.clear();
        GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixelBuffer);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();

        pixelBuffer.rewind();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int srcIdx = ((height - 1 - y) * width + x) * 4;
                int dstIdx = (y * width + x) * 3;

                byte r = pixelBuffer.get(srcIdx);
                byte g = pixelBuffer.get(srcIdx + 1);
                byte b = pixelBuffer.get(srcIdx + 2);

                pixels[dstIdx] = b;
                pixels[dstIdx + 1] = g;
                pixels[dstIdx + 2] = r;
            }
        }

        return image;
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        this.pixelBuffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder());
    }
}
