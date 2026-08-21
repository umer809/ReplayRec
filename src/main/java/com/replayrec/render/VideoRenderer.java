package com.replayrec.render;

import com.replayrec.ReplayRecMod;
import com.replayrec.recording.RecordingFrame;
import org.jcodec.api.awt.AWTSequenceEncoder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class VideoRenderer {
    private static VideoRenderer INSTANCE;
    private volatile boolean rendering;
    private volatile float renderProgress;
    private Thread renderThread;

    public static VideoRenderer getInstance() {
        if (INSTANCE == null) INSTANCE = new VideoRenderer();
        return INSTANCE;
    }

    public void renderVideo(List<RecordingFrame> frames, String outputPath, RenderSettings settings) {
        if (rendering) return;

        rendering = true;
        renderProgress = 0;

        renderThread = new Thread(() -> performRender(frames, outputPath, settings), "ReplayRec-Render");
        renderThread.setDaemon(true);
        renderThread.start();
    }

    private void performRender(List<RecordingFrame> frames, String outputPath, RenderSettings settings) {
        try {
            File outputFile = new File(outputPath);
            outputFile.getParentFile().mkdirs();

            int startFrame = Math.max(0, settings.startFrame);
            int endFrame = settings.endFrame < 0 ? frames.size() : Math.min(settings.endFrame, frames.size());
            int totalFrames = endFrame - startFrame;

            if (totalFrames <= 0) {
                ReplayRecMod.LOGGER.warn("No frames to render");
                rendering = false;
                return;
            }

            AWTSequenceEncoder encoder = AWTSequenceEncoder.createSequenceEncoder(outputFile, settings.fps);

            for (int i = startFrame; i < endFrame; i++) {
                RecordingFrame frame = frames.get(i);
                BufferedImage image = getFrameImage(frame);

                if (image != null) {
                    encoder.encodeImage(image);
                }

                renderProgress = (float) (i - startFrame + 1) / totalFrames;
            }

            encoder.finish();
            ReplayRecMod.LOGGER.info("Video exported: {} ({} frames)", outputPath, totalFrames);

        } catch (Exception e) {
            ReplayRecMod.LOGGER.error("Video render failed", e);
        } finally {
            rendering = false;
            renderProgress = 0;
        }
    }

    private BufferedImage getFrameImage(RecordingFrame frame) {
        if (frame.image != null) {
            return frame.image;
        }

        File frameFile = new File("frame_" + frame.frameNumber + ".png");
        try {
            return ImageIO.read(frameFile);
        } catch (IOException e) {
            ReplayRecMod.LOGGER.error("Failed to load frame {} from disk", frame.frameNumber);
            return null;
        }
    }

    public boolean isRendering() { return rendering; }
    public float getRenderProgress() { return renderProgress; }
}
