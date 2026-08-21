package com.replayrec.gui;

import com.replayrec.config.ModConfig;
import com.replayrec.recording.RecordingManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class RenderSettingsScreen extends Screen {
    private final Screen parent;
    private final ModConfig config;
    private String fpsText;
    private String qualityText;
    private String bitrateText;

    public RenderSettingsScreen(Screen parent) {
        super(Text.literal("Render Settings"));
        this.parent = parent;
        this.config = ModConfig.getInstance();
    }

    @Override
    protected void init() {
        fpsText = String.valueOf(config.recordingFPS);
        qualityText = String.valueOf(config.recordingQuality);
        bitrateText = String.valueOf(config.videoBitrate);

        int centerX = this.width / 2;
        int startY = 40;
        int spacing = 24;

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("FPS: " + config.recordingFPS),
                button -> {
                    cycleFps();
                    button.setMessage(Text.literal("FPS: " + config.recordingFPS));
                }
        ).dimensions(centerX - 100, startY, 200, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Quality: " + config.recordingQuality),
                button -> {
                    cycleQuality();
                    button.setMessage(Text.literal("Quality: " + config.recordingQuality));
                }
        ).dimensions(centerX - 100, startY + spacing, 200, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Bitrate: " + config.videoBitrate + " kbps"),
                button -> {
                    cycleBitrate();
                    button.setMessage(Text.literal("Bitrate: " + config.videoBitrate + " kbps"));
                }
        ).dimensions(centerX - 100, startY + spacing * 2, 200, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Audio: " + (config.recordAudio ? "ON" : "OFF")),
                button -> {
                    config.recordAudio = !config.recordAudio;
                    config.save();
                    button.setMessage(Text.literal("Audio: " + (config.recordAudio ? "ON" : "OFF")));
                }
        ).dimensions(centerX - 100, startY + spacing * 3, 200, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Microphone: " + (config.recordMicrophone ? "ON" : "OFF")),
                button -> {
                    config.recordMicrophone = !config.recordMicrophone;
                    config.save();
                    button.setMessage(Text.literal("Microphone: " + (config.recordMicrophone ? "ON" : "OFF")));
                }
        ).dimensions(centerX - 100, startY + spacing * 4, 200, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Save & Back"),
                button -> this.close()
        ).dimensions(centerX - 100, startY + spacing * 6, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, "Change export quality settings", this.width / 2 - 80, 28, 0xAAAAAA);
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }

    private void cycleFps() {
        int[] options = {24, 30, 60, 120};
        int idx = -1;
        for (int i = 0; i < options.length; i++) {
            if (options[i] == config.recordingFPS) { idx = i; break; }
        }
        config.recordingFPS = options[(idx + 1) % options.length];
        config.save();
    }

    private void cycleQuality() {
        int[] options = {20, 40, 60, 80, 100};
        int idx = -1;
        for (int i = 0; i < options.length; i++) {
            if (options[i] == config.recordingQuality) { idx = i; break; }
        }
        config.recordingQuality = options[(idx + 1) % options.length];
        config.save();
    }

    private void cycleBitrate() {
        int[] options = {5000, 10000, 15000, 20000, 50000};
        int idx = -1;
        for (int i = 0; i < options.length; i++) {
            if (options[i] == config.videoBitrate) { idx = i; break; }
        }
        config.videoBitrate = options[(idx + 1) % options.length];
        config.save();
    }
}
