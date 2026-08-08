package com.replayrec.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.replayrec.ReplayRecMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("replayrec.json");
    private static ModConfig INSTANCE;

    private int width = 1920;
    private int height = 1080;
    private int fps = 120;
    private int bitrate = 20000;
    private String encoder = "auto";
    private String containerFormat = "mp4";
    private String outputDir = "replayrec";
    private int audioBitrate = 192;
    private int audioSampleRate = 48000;
    private boolean captureGameAudio = true;
    private boolean captureMicrophone = false;
    private int ringBufferSeconds = 30;
    private boolean enableInstantReplay = false;

    public static ModConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ModConfig();
        }
        return INSTANCE;
    }

    public void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
                this.width = loaded.width;
                this.height = loaded.height;
                this.fps = loaded.fps;
                this.bitrate = loaded.bitrate;
                this.encoder = loaded.encoder;
                this.containerFormat = loaded.containerFormat;
                this.outputDir = loaded.outputDir;
                this.audioBitrate = loaded.audioBitrate;
                this.audioSampleRate = loaded.audioSampleRate;
                this.captureGameAudio = loaded.captureGameAudio;
                this.captureMicrophone = loaded.captureMicrophone;
                this.ringBufferSeconds = loaded.ringBufferSeconds;
                this.enableInstantReplay = loaded.enableInstantReplay;
                ReplayRecMod.LOGGER.info("Config loaded from {}", CONFIG_PATH);
            } catch (IOException e) {
                ReplayRecMod.LOGGER.error("Failed to load config", e);
            }
        } else {
            save();
        }
    }

    public void save() {
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(this, writer);
            ReplayRecMod.LOGGER.info("Config saved to {}", CONFIG_PATH);
        } catch (IOException e) {
            ReplayRecMod.LOGGER.error("Failed to save config", e);
        }
    }

    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }

    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }

    public int getFps() { return fps; }
    public void setFps(int fps) { this.fps = fps; }

    public int getBitrate() { return bitrate; }
    public void setBitrate(int bitrate) { this.bitrate = bitrate; }

    public String getEncoder() { return encoder; }
    public void setEncoder(String encoder) { this.encoder = encoder; }

    public String getContainerFormat() { return containerFormat; }
    public void setContainerFormat(String containerFormat) { this.containerFormat = containerFormat; }

    public String getOutputDir() { return outputDir; }
    public void setOutputDir(String outputDir) { this.outputDir = outputDir; }

    public int getAudioBitrate() { return audioBitrate; }
    public void setAudioBitrate(int audioBitrate) { this.audioBitrate = audioBitrate; }

    public int getAudioSampleRate() { return audioSampleRate; }
    public void setAudioSampleRate(int audioSampleRate) { this.audioSampleRate = audioSampleRate; }

    public boolean isCaptureGameAudio() { return captureGameAudio; }
    public void setCaptureGameAudio(boolean captureGameAudio) { this.captureGameAudio = captureGameAudio; }

    public boolean isCaptureMicrophone() { return captureMicrophone; }
    public void setCaptureMicrophone(boolean captureMicrophone) { this.captureMicrophone = captureMicrophone; }

    public int getRingBufferSeconds() { return ringBufferSeconds; }
    public void setRingBufferSeconds(int ringBufferSeconds) { this.ringBufferSeconds = ringBufferSeconds; }

    public boolean isEnableInstantReplay() { return enableInstantReplay; }
    public void setEnableInstantReplay(boolean enableInstantReplay) { this.enableInstantReplay = enableInstantReplay; }
}
