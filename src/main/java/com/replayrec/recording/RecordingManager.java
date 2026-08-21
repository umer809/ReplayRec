package com.replayrec.recording;

import com.replayrec.ReplayRecMod;
import com.replayrec.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RecordingManager {
    private static RecordingManager INSTANCE;
    private final ModConfig config;
    private final ConcurrentLinkedQueue<RecordingFrame> frameBuffer = new ConcurrentLinkedQueue<>();
    private final List<RecordingFrame> savedFrames = new ArrayList<>();

    private volatile boolean recording;
    private Thread recordingThread;
    private int frameCount;
    private long recordingStartTime;
    private Path currentRecordingPath;

    private static final int THUMB_WIDTH = 320;
    private static final int THUMB_HEIGHT = 180;

    public static RecordingManager getInstance() {
        if (INSTANCE == null) INSTANCE = new RecordingManager();
        return INSTANCE;
    }

    private RecordingManager() {
        this.config = ModConfig.getInstance();
    }

    public void startRecording() {
        if (recording) return;

        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            currentRecordingPath = Path.of(config.outputDirectory, "recording_" + timestamp);
            Files.createDirectories(currentRecordingPath);
        } catch (IOException e) {
            ReplayRecMod.LOGGER.error("Failed to create recording directory", e);
            return;
        }

        frameBuffer.clear();
        savedFrames.clear();
        frameCount = 0;
        recording = true;
        recordingStartTime = System.currentTimeMillis();

        recordingThread = new Thread(this::recordingLoop, "ReplayRec-Recording");
        recordingThread.setDaemon(true);
        recordingThread.start();

        ReplayRecMod.LOGGER.info("Recording started");
    }

    public void stopRecording() {
        if (!recording) return;
        recording = false;

        if (recordingThread != null) {
            try {
                recordingThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        quickSave();
        long duration = (System.currentTimeMillis() - recordingStartTime) / 1000;
        ReplayRecMod.LOGGER.info("Recording stopped. {} frames, {}s", frameCount, duration);
    }

    public void quickSave() {
        for (RecordingFrame frame : frameBuffer) {
            if (!frame.savedToDisk) {
                saveFrameToDisk(frame);
            }
        }
        savedFrames.addAll(frameBuffer);
        frameBuffer.clear();
    }

    private void recordingLoop() {
        MinecraftClient client = MinecraftClient.getInstance();
        long frameInterval = 1000L / Math.min(config.recordingFPS, 15);
        long maxDuration = config.maxRecordingMinutes * 60L * 1000L;

        while (recording) {
            long loopStart = System.currentTimeMillis();

            if (System.currentTimeMillis() - recordingStartTime >= maxDuration) {
                ReplayRecMod.LOGGER.info("Max recording time reached");
                client.execute(this::stopRecording);
                break;
            }

            client.execute(() -> {
                if (!recording || client.player == null) return;
                captureThumbnail(client);
            });

            long elapsed = System.currentTimeMillis() - loopStart;
            long sleepTime = frameInterval - elapsed;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    private void captureThumbnail(MinecraftClient client) {
        try {
            int fbWidth = client.getWindow().getFramebufferWidth();
            int fbHeight = client.getWindow().getFramebufferHeight();

            float scaleX = (float) THUMB_WIDTH / fbWidth;
            float scaleY = (float) THUMB_HEIGHT / fbHeight;
            float scale = Math.min(scaleX, scaleY);
            int readWidth = (int) (fbWidth * scale);
            int readHeight = (int) (fbHeight * scale);

            ByteBuffer buffer = ByteBuffer.allocateDirect(readWidth * readHeight * 4).order(ByteOrder.nativeOrder());

            GL11.glReadPixels(0, 0, readWidth, readHeight, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

            BufferedImage image = new BufferedImage(THUMB_WIDTH, THUMB_HEIGHT, BufferedImage.TYPE_INT_RGB);
            buffer.rewind();
            for (int y = 0; y < readHeight; y++) {
                for (int x = 0; x < readWidth; x++) {
                    int srcIdx = ((readHeight - 1 - y) * readWidth + x) * 4;
                    int r = buffer.get(srcIdx) & 0xFF;
                    int g = buffer.get(srcIdx + 1) & 0xFF;
                    int b = buffer.get(srcIdx + 2) & 0xFF;
                    int dstX = (int) (x / scale);
                    int dstY = (int) (y / scale);
                    if (dstX < THUMB_WIDTH && dstY < THUMB_HEIGHT) {
                        image.setRGB(dstX, dstY, (r << 16) | (g << 8) | b);
                    }
                }
            }

            RecordingFrame frame = new RecordingFrame(
                    frameCount++,
                    System.currentTimeMillis() - recordingStartTime,
                    image, THUMB_WIDTH, THUMB_HEIGHT
            );

            frameBuffer.offer(frame);

            if (frameBuffer.size() > config.maxBufferSize) {
                RecordingFrame oldest = frameBuffer.poll();
                if (oldest != null) {
                    saveFrameToDisk(oldest);
                    savedFrames.add(oldest);
                }
            }
        } catch (Exception e) {
            ReplayRecMod.LOGGER.error("Failed to capture frame", e);
        }
    }

    private void saveFrameToDisk(RecordingFrame frame) {
        if (frame.savedToDisk || frame.image == null) return;
        try {
            Path framePath = currentRecordingPath.resolve("frame_" + frame.frameNumber + ".png");
            ImageIO.write(frame.image, "png", framePath.toFile());
            frame.savedToDisk = true;
            frame.image = null;
        } catch (IOException e) {
            ReplayRecMod.LOGGER.error("Failed to save frame {}", frame.frameNumber, e);
        }
    }

    public List<RecordingFrame> getAllFrames() {
        List<RecordingFrame> all = new ArrayList<>(savedFrames);
        all.addAll(frameBuffer);
        return all;
    }

    public Path getCurrentRecordingPath() { return currentRecordingPath; }
    public boolean isRecording() { return recording; }
    public int getFrameCount() { return frameCount; }
    public long getRecordingDurationMs() {
        return recording ? System.currentTimeMillis() - recordingStartTime : 0;
    }
}
