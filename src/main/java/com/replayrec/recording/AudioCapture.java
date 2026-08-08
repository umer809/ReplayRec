package com.replayrec.recording;

import com.replayrec.ReplayRecMod;
import com.replayrec.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;

import javax.sound.sampled.*;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class AudioCapture {
    private AudioCaptureThread captureThread;
    private boolean isCapturing;
    private final ModConfig config;

    public AudioCapture() {
        this.config = ModConfig.getInstance();
    }

    public void start(Consumer<short[]> audioCallback) {
        if (isCapturing) return;
        isCapturing = true;
        captureThread = new AudioCaptureThread(audioCallback);
        captureThread.start();
        ReplayRecMod.LOGGER.info("Audio capture started");
    }

    public void stop() {
        isCapturing = false;
        if (captureThread != null) {
            captureThread.interrupt();
            try {
                captureThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            captureThread = null;
        }
        ReplayRecMod.LOGGER.info("Audio capture stopped");
    }

    @FunctionalInterface
    public interface Consumer<T> {
        void accept(T t);
    }

    private class AudioCaptureThread extends Thread {
        private final Consumer<short[]> callback;
        private volatile boolean running = true;

        AudioCaptureThread(Consumer<short[]> callback) {
            this.callback = callback;
            setName("ReplayRec-AudioCapture");
            setDaemon(true);
        }

        @Override
        public void run() {
            try {
                AudioFormat format = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    config.getAudioSampleRate(),
                    16,
                    2,
                    4,
                    config.getAudioSampleRate(),
                    false
                );

                DataLine.Info info = new DataLine.Info(Clip.class, format);
                if (!AudioSystem.isLineSupported(info)) {
                    ReplayRecMod.LOGGER.warn("Audio format not supported, falling back to default");
                    format = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        44100,
                        16,
                        2,
                        4,
                        44100,
                        false
                    );
                    info = new DataLine.Info(Clip.class, format);
                }

                TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
                line.open(format);
                line.start();

                byte[] buffer = new byte[4096];
                int sampleRate = (int) format.getSampleRate();
                int channels = format.getChannels();

                while (running && !Thread.currentThread().isInterrupted()) {
                    int bytesRead = line.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        short[] samples = bytesToShorts(buffer, bytesRead);
                        callback.accept(samples);
                    }
                }

                line.stop();
                line.close();
            } catch (LineUnavailableException e) {
                ReplayRecMod.LOGGER.error("Audio capture failed", e);
            }
        }

        private short[] bytesToShorts(byte[] bytes, int length) {
            int shortCount = length / 2;
            short[] shorts = new short[shortCount];
            for (int i = 0; i < shortCount; i++) {
                shorts[i] = (short) ((bytes[i * 2] & 0xFF) | (bytes[i * 2 + 1] << 8));
            }
            return shorts;
        }

        public void shutdown() {
            running = false;
        }
    }

    public boolean isCapturing() { return isCapturing; }
}
