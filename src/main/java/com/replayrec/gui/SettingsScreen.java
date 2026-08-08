package com.replayrec.gui;

import com.replayrec.config.ModConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class SettingsScreen extends Screen {
    private final ModConfig config = ModConfig.getInstance();
    private final Screen parent;

    public SettingsScreen(Screen parent) {
        super(Text.literal("ReplayRec Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = 40;
        int spacing = 24;

        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Resolution: " + config.getWidth() + "x" + config.getHeight()),
            button -> {
                cycleResolution();
                button.setMessage(Text.literal("Resolution: " + config.getWidth() + "x" + config.getHeight()));
            }
        ).dimensions(centerX - 100, startY, 200, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("FPS: " + config.getFps()),
            button -> {
                cycleFps();
                button.setMessage(Text.literal("FPS: " + config.getFps()));
            }
        ).dimensions(centerX - 100, startY + spacing, 200, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Encoder: " + config.getEncoder()),
            button -> {
                cycleEncoder();
                button.setMessage(Text.literal("Encoder: " + config.getEncoder()));
            }
        ).dimensions(centerX - 100, startY + spacing * 2, 200, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Bitrate: " + config.getBitrate() + " kbps"),
            button -> {
                cycleBitrate();
                button.setMessage(Text.literal("Bitrate: " + config.getBitrate() + " kbps"));
            }
        ).dimensions(centerX - 100, startY + spacing * 3, 200, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Format: " + config.getContainerFormat()),
            button -> {
                cycleFormat();
                button.setMessage(Text.literal("Format: " + config.getContainerFormat()));
            }
        ).dimensions(centerX - 100, startY + spacing * 4, 200, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Game Audio: " + (config.isCaptureGameAudio() ? "ON" : "OFF")),
            button -> {
                config.setCaptureGameAudio(!config.isCaptureGameAudio());
                config.save();
                button.setMessage(Text.literal("Game Audio: " + (config.isCaptureGameAudio() ? "ON" : "OFF")));
            }
        ).dimensions(centerX - 100, startY + spacing * 5, 200, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Microphone: " + (config.isCaptureMicrophone() ? "ON" : "OFF")),
            button -> {
                config.setCaptureMicrophone(!config.isCaptureMicrophone());
                config.save();
                button.setMessage(Text.literal("Microphone: " + (config.isCaptureMicrophone() ? "ON" : "OFF")));
            }
        ).dimensions(centerX - 100, startY + spacing * 6, 200, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Done"),
            button -> this.close()
        ).dimensions(centerX - 100, startY + spacing * 8, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }

    private void cycleResolution() {
        int[][] resolutions = {{640, 360}, {1280, 720}, {1920, 1080}, {2560, 1440}, {3840, 2160}};
        int current = -1;
        for (int i = 0; i < resolutions.length; i++) {
            if (resolutions[i][0] == config.getWidth() && resolutions[i][1] == config.getHeight()) {
                current = i;
                break;
            }
        }
        int next = (current + 1) % resolutions.length;
        config.setWidth(resolutions[next][0]);
        config.setHeight(resolutions[next][1]);
        config.save();
    }

    private void cycleFps() {
        int[] fpsOptions = {30, 60, 120, 144, 240};
        int current = -1;
        for (int i = 0; i < fpsOptions.length; i++) {
            if (fpsOptions[i] == config.getFps()) {
                current = i;
                break;
            }
        }
        int next = (current + 1) % fpsOptions.length;
        config.setFps(fpsOptions[next]);
        config.save();
    }

    private void cycleEncoder() {
        String[] encoders = {"auto", "h264_nvenc", "h265_nvenc", "libx264"};
        int current = -1;
        for (int i = 0; i < encoders.length; i++) {
            if (encoders[i].equals(config.getEncoder())) {
                current = i;
                break;
            }
        }
        int next = (current + 1) % encoders.length;
        config.setEncoder(encoders[next]);
        config.save();
    }

    private void cycleBitrate() {
        int[] bitrates = {5000, 10000, 15000, 20000, 30000, 50000};
        int current = -1;
        for (int i = 0; i < bitrates.length; i++) {
            if (bitrates[i] == config.getBitrate()) {
                current = i;
                break;
            }
        }
        int next = (current + 1) % bitrates.length;
        config.setBitrate(bitrates[next]);
        config.save();
    }

    private void cycleFormat() {
        String[] formats = {"mp4", "mkv", "webm"};
        int current = -1;
        for (int i = 0; i < formats.length; i++) {
            if (formats[i].equals(config.getContainerFormat())) {
                current = i;
                break;
            }
        }
        int next = (current + 1) % formats.length;
        config.setContainerFormat(formats[next]);
        config.save();
    }
}
