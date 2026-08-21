package com.replayrec.render;

import java.awt.image.BufferedImage;

public class RenderSettings {
    public int fps = 60;
    public int quality = 80;
    public int bitrate = 10000;
    public String codec = "h264";
    public boolean includeAudio = true;
    public int startFrame = 0;
    public int endFrame = -1;

    public RenderSettings() {}

    public RenderSettings(int fps, int quality, int bitrate) {
        this.fps = fps;
        this.quality = quality;
        this.bitrate = bitrate;
    }
}
