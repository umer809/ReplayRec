package com.replayrec.recording;

import com.replayrec.ReplayRecMod;
import com.replayrec.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import org.lwjgl.opengl.GL11;

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
        long frameTime = 1000L / config.recordingFPS;
        long maxDuration = config.maxRecordingMinutes * 60L * 1000L;

        while (recording) {
            long loopStart = System.currentTimeMillis();

            if (System.currentTimeMillis() - recordingStartTime >= maxDuration) {
                ReplayRecMod.LOGGER.info("Max recording time reached, stopping");
                client.execute(this::stopRecording);
                break;
            }

            client.execute(() -> {
                if (!recording || client.player == null || client.gameRenderer == null) return;
                captureFrame(client);
            });

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
            int width = client.getWindow().getFramebufferWidth();
            int height = client.getWindow().getFramebufferHeight();

            NativeImage image = captureFramebuffer(width, height);

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

    private NativeImage captureFramebuffer(int width, int height) {
        NativeImage image = new NativeImage(width, height, true);
        ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder());

        GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

        buffer.rewind();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int srcIdx = ((height - 1 - y) * width + x) * 4;
                byte r = buffer.get(srcIdx);
                byte g = buffer.get(srcIdx + 1);
                byte b = buffer.get(srcIdx + 2);
                int color = ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
                image.setColor(x, y, color);
            }
        }

        return image;
    }

    private void saveFrameToDisk(RecordingFrame frame) {
        if (frame.savedToDisk || frame.image == null) return;
        try {
            Path framePath = currentRecordingPath.resolve("frame_" + frame.frameNumber + ".png");
            frame.image.writeTo(framePath);
            frame.savedToDisk = true;
            frame.image.close();
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
