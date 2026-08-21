package com.replayrec.recording;

import java.awt.image.BufferedImage;

public class RecordingFrame {
    public final int frameNumber;
    public final long timestamp;
    public BufferedImage image;
    public final int width;
    public final int height;
    public boolean savedToDisk;

    public RecordingFrame(int frameNumber, long timestamp, BufferedImage image, int width, int height) {
        this.frameNumber = frameNumber;
        this.timestamp = timestamp;
        this.image = image;
        this.width = width;
        this.height = height;
        this.savedToDisk = false;
    }
}
