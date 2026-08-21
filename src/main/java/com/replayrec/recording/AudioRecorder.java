package com.replayrec.recording;

import com.replayrec.ReplayRecMod;
import com.replayrec.config.ModConfig;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AudioRecorder {
    private static AudioRecorder INSTANCE;
    private final ModConfig config;
    private volatile boolean recording;
    private Thread recordThread;
    private TargetDataLine micLine;
    private File outputFile;

    public static AudioRecorder getInstance() {
        if (INSTANCE == null) INSTANCE = new AudioRecorder();
        return INSTANCE;
    }

    private AudioRecorder() {
        this.config = ModConfig.getInstance();
    }

    public void startRecording(boolean recordMicrophone) {
        if (recording || !recordMicrophone) return;
        if (!config.recordAudio) return;

        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            outputFile = new File(config.outputDirectory, "audio_" + timestamp + ".wav");
            outputFile.getParentFile().mkdirs();

            recording = true;
            recordThread = new Thread(this::recordAudio, "ReplayRec-Audio");
            recordThread.setDaemon(true);
            recordThread.start();

            ReplayRecMod.LOGGER.info("Audio recording started");
        } catch (Exception e) {
            ReplayRecMod.LOGGER.error("Failed to start audio recording", e);
        }
    }

    private void recordAudio() {
        try {
            AudioFormat format = new AudioFormat(44100, 16, 2, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                ReplayRecMod.LOGGER.warn("Audio format not supported");
                recording = false;
                return;
            }

            micLine = (TargetDataLine) AudioSystem.getLine(info);
            micLine.open(format);
            micLine.start();

            AudioInputStream ais = new AudioInputStream(micLine);
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, outputFile);

        } catch (LineUnavailableException | IOException e) {
            ReplayRecMod.LOGGER.error("Audio recording failed", e);
        } finally {
            recording = false;
        }
    }

    public void stopRecording() {
        if (!recording) return;
        recording = false;

        if (micLine != null) {
            micLine.stop();
            micLine.close();
        }

        if (recordThread != null) {
            try {
                recordThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        ReplayRecMod.LOGGER.info("Audio recording stopped");
    }

    public boolean isRecording() { return recording; }
}
