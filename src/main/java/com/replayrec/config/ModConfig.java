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

    public int recordingFPS = 60;
    public int recordingQuality = 80;
    public int maxRecordingMinutes = 10;
    public boolean recordAudio = true;
    public boolean recordMicrophone = false;
    public String outputFormat = "mp4";
    public int videoBitrate = 10000;
    public int audioBitrate = 192;
    public boolean autoSave = true;
    public int autoSaveInterval = 300;
    public String videoCodec = "h264";
    public String audioCodec = "aac";
    public boolean recordHUD = true;
    public int maxBufferSize = 2048;
    public String outputDirectory = "recordings";
    public boolean enableShaderSwitch = true;
    public boolean enableResourcePackSwitch = true;
    public int renderThreads = 4;

    public static ModConfig getInstance() {
        if (INSTANCE == null) INSTANCE = new ModConfig();
        return INSTANCE;
    }

    public void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
                this.recordingFPS = loaded.recordingFPS;
                this.recordingQuality = loaded.recordingQuality;
                this.maxRecordingMinutes = loaded.maxRecordingMinutes;
                this.recordAudio = loaded.recordAudio;
                this.recordMicrophone = loaded.recordMicrophone;
                this.outputFormat = loaded.outputFormat;
                this.videoBitrate = loaded.videoBitrate;
                this.audioBitrate = loaded.audioBitrate;
                this.autoSave = loaded.autoSave;
                this.autoSaveInterval = loaded.autoSaveInterval;
                this.videoCodec = loaded.videoCodec;
                this.audioCodec = loaded.audioCodec;
                this.recordHUD = loaded.recordHUD;
                this.maxBufferSize = loaded.maxBufferSize;
                this.outputDirectory = loaded.outputDirectory;
                this.enableShaderSwitch = loaded.enableShaderSwitch;
                this.enableResourcePackSwitch = loaded.enableResourcePackSwitch;
                this.renderThreads = loaded.renderThreads;
                ReplayRecMod.LOGGER.info("Config loaded");
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
        } catch (IOException e) {
            ReplayRecMod.LOGGER.error("Failed to save config", e);
        }
    }
}
