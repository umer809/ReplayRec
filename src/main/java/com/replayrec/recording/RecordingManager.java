package com.replayrec.recording;

import com.replayrec.ReplayRecMod;
import com.replayrec.config.ModConfig;
import net.minecraft.client.MinecraftClient;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class RecordingManager {
    private static RecordingManager INSTANCE;
    private final ModConfig config;
    private final VideoEncoder videoEncoder;
    private final AudioCapture audioCapture;
    private final BlockingQueue<Runnable> frameQueue;

    private volatile boolean isRecording;
    private volatile boolean isPaused;
    private Thread recordingThread;
    private long frameCount;
    private long startTime;

    public static RecordingManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new RecordingManager();
        }
        return INSTANCE;
    }

    private RecordingManager() {
        this.config = ModConfig.getInstance();
        this.videoEncoder = new VideoEncoder();
        this.audioCapture = new AudioCapture();
        this.frameQueue = new LinkedBlockingQueue<>(config.getFps() * 2);
    }

    public void startRecording() {
        if (isRecording) return;

        MinecraftClient client = MinecraftClient.getInstance();
        int width = client.getWindow().getFramebufferWidth();
        int height = client.getWindow().getFramebufferHeight();

        try {
            videoEncoder.start("recording");
            isRecording = true;
            isPaused = false;
            frameCount = 0;
            startTime = System.currentTimeMillis();

            if (config.isCaptureGameAudio()) {
                audioCapture.start(samples -> {
                    if (isRecording && !isPaused) {
                        videoEncoder.encodeAudioSamples(
                            samples,
                            config.getAudioSampleRate(),
                            2
                        );
                    }
                });
            }

            recordingThread = new Thread(this::recordingLoop, "ReplayRec-Recording");
            recordingThread.setDaemon(true);
            recordingThread.start();

            ReplayRecMod.LOGGER.info("Recording started");
        } catch (Exception e) {
            ReplayRecMod.LOGGER.error("Failed to start recording", e);
            isRecording = false;
        }
    }

    public void stopRecording() {
        if (!isRecording) return;

        isRecording = false;
        audioCapture.stop();

        if (recordingThread != null) {
            recordingThread.interrupt();
            try {
                recordingThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        videoEncoder.stop();
        long duration = System.currentTimeMillis() - startTime;
        ReplayRecMod.LOGGER.info("Recording stopped. Duration: {}s, Frames: {}", duration / 1000, frameCount);
    }

    public void togglePause() {
        if (!isRecording) return;
        isPaused = !isPaused;
        ReplayRecMod.LOGGER.info("Recording {}", isPaused ? "paused" : "resumed");
    }

    public void enqueueFrame(Runnable frameCapture) {
        if (!isRecording || isPaused) return;
        if (!frameQueue.offer(frameCapture)) {
            ReplayRecMod.LOGGER.debug("Frame queue full, dropping frame");
        }
    }

    private void recordingLoop() {
        MinecraftClient client = MinecraftClient.getInstance();
        FrameCapture frameCapture = new FrameCapture(
            client.getWindow().getFramebufferWidth(),
            client.getWindow().getFramebufferHeight()
        );

        long frameInterval = 1_000_000_000L / config.getFps();

        while (isRecording && !Thread.currentThread().isInterrupted()) {
            try {
                long frameStart = System.nanoTime();

                Runnable capture = frameQueue.poll(16, TimeUnit.MILLISECONDS);
                if (capture != null) {
                    capture.run();
                }

                client.execute(() -> {
                    try {
                        java.awt.image.BufferedImage frame = frameCapture.captureFrame();
                        videoEncoder.encodeFrame(frame);
                    } catch (Exception e) {
                        ReplayRecMod.LOGGER.error("Frame capture failed", e);
                    }
                });

                frameCount++;

                long elapsed = System.nanoTime() - frameStart;
                long sleepNanos = frameInterval - elapsed;
                if (sleepNanos > 0) {
                    TimeUnit.NANOSECONDS.sleep(sleepNanos);
                }
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                ReplayRecMod.LOGGER.error("Recording error", e);
            }
        }
    }

    public boolean isRecording() { return isRecording; }
    public boolean isPaused() { return isPaused; }
    public long getFrameCount() { return frameCount; }
    public long getRecordingDurationMs() {
        return isRecording ? System.currentTimeMillis() - startTime : 0;
    }
    public double getCurrentFps() {
        long duration = getRecordingDurationMs();
        return duration > 0 ? (frameCount * 1000.0) / duration : 0;
    }
}
