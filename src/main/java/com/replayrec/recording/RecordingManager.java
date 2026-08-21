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
    private int tickSkipCounter;
    private long recordingStartTime;
    private Path currentRecordingPath;

    private static final int CAPTURE_SCALE = 2;

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
        tickSkipCounter = 0;
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
        long frameTime = 1000L / config.recordingFPS;
        long maxDuration = config.maxRecordingMinutes * 60L * 1000L;

        while (recording) {
            long loopStart = System.currentTimeMillis();

            if (System.currentTimeMillis() - recordingStartTime >= maxDuration) {
                ReplayRecMod.LOGGER.info("Max recording time reached, stopping");
                client.execute(this::stopRecording);
                break;
            }

            tickSkipCounter++;
            if (tickSkipCounter % 2 == 0) {
                client.execute(() -> {
                    if (!recording || client.player == null || client.gameRenderer == null) return;
                    captureFrame(client);
                });
            }

            long elapsed = System.currentTimeMillis() - loopStart;
            long sleepTime = frameTime - elapsed;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    private void captureFrame(MinecraftClient client) {
        try {
            int fullWidth = client.getWindow().getFramebufferWidth();
            int fullHeight = client.getWindow().getFramebufferHeight();
            int width = fullWidth / CAPTURE_SCALE;
            int height = fullHeight / CAPTURE_SCALE;

            BufferedImage image = captureFramebuffer(width, height);

            RecordingFrame frame = new RecordingFrame(
                    frameCount++,
                    System.currentTimeMillis() - recordingStartTime,
                    image, width, height
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

    private BufferedImage captureFramebuffer(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder());

        GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

        buffer.rewind();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int srcIdx = ((height - 1 - y) * width + x) * 4;
                int r = buffer.get(srcIdx) & 0xFF;
                int g = buffer.get(srcIdx + 1) & 0xFF;
                int b = buffer.get(srcIdx + 2) & 0xFF;
                image.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }

        return image;
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
