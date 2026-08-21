package com.replayrec.recording;

import net.minecraft.client.texture.NativeImage;

import java.awt.image.BufferedImage;

public class RecordingFrame {
    public final int frameNumber;
    public final long timestamp;
    public NativeImage image;
    public final int width;
    public final int height;
    public boolean savedToDisk;

    public RecordingFrame(int frameNumber, long timestamp, NativeImage image, int width, int height) {
        this.frameNumber = frameNumber;
        this.timestamp = timestamp;
        this.image = image;
        this.width = width;
        this.height = height;
        this.savedToDisk = false;
    }

    public BufferedImage toBufferedImage() {
        if (image == null) return null;
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = image.getColor(x, y);
                int r = (color >> 16) & 0xFF;
                int g = (color >> 8) & 0xFF;
                int b = color & 0xFF;
                bufferedImage.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        return bufferedImage;
    }
}
