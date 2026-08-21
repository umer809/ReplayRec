package com.replayrec.gui;

import com.replayrec.ReplayRecMod;
import com.replayrec.recording.RecordingFrame;
import com.replayrec.recording.RecordingManager;
import com.replayrec.render.RenderSettings;
import com.replayrec.render.VideoRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class TimelineScreen extends Screen {
    private final RecordingManager recordingManager;
    private List<RecordingFrame> frames;
    private int currentFrame = 0;
    private boolean playing = false;
    private long lastFrameTime = 0;
    private int selectionStart = -1;
    private int selectionEnd = -1;
    private boolean selecting = false;

    private static final int TIMELINE_HEIGHT = 30;

    public TimelineScreen() {
        super(Text.literal("ReplayRec Editor"));
        this.recordingManager = RecordingManager.getInstance();
        this.frames = new ArrayList<>(recordingManager.getAllFrames());
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int btnY = this.height - 40;
        int btnWidth = 60;
        int spacing = 65;
        int startX = centerX - (spacing * 3 + btnWidth) / 2;

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal(playing ? "Pause" : "Play"),
                button -> {
                    togglePlayback();
                    button.setMessage(Text.literal(playing ? "Pause" : "Play"));
                }
        ).dimensions(startX, btnY, btnWidth, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Export"),
                button -> exportVideo()
        ).dimensions(startX + spacing, btnY, btnWidth, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Cut"),
                button -> cutSelection()
        ).dimensions(startX + spacing * 2, btnY, btnWidth, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Settings"),
                button -> client.setScreen(new RenderSettingsScreen(this))
        ).dimensions(startX + spacing * 3, btnY, btnWidth, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Close"),
                button -> this.close()
        ).dimensions(startX + spacing * 4, btnY, btnWidth, 20).build());
    }

    @Override
    public void tick() {
        super.tick();
        if (playing && !frames.isEmpty()) {
            long now = System.currentTimeMillis();
            long frameInterval = 1000L / 60;
            if (now - lastFrameTime >= frameInterval) {
                currentFrame++;
                if (currentFrame >= frames.size()) {
                    currentFrame = 0;
                }
                lastFrameTime = now;
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        if (frames.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, "No recording loaded", this.width / 2, this.height / 2, 0xFF5555);
            return;
        }

        int previewWidth = Math.min(this.width - 40, 640);
        int previewHeight = previewWidth * 9 / 16;
        int previewX = (this.width - previewWidth) / 2;
        int previewY = 30;

        context.fill(previewX, previewY, previewX + previewWidth, previewY + previewHeight, 0xFF000000);

        if (!frames.isEmpty() && currentFrame < frames.size()) {
            RecordingFrame frame = frames.get(currentFrame);
            String frameInfo = String.format("Frame %d / %d", currentFrame + 1, frames.size());
            context.drawCenteredTextWithShadow(this.textRenderer, frameInfo,
                    this.width / 2, previewY + previewHeight + 5, 0xFFFFFF);
        }

        String info = String.format("Frames: %d | Duration: %s",
                frames.size(),
                formatDuration(frames.size()));
        context.drawCenteredTextWithShadow(this.textRenderer, info,
                this.width / 2, previewY + previewHeight + 18, 0xAAAAAA);

        renderTimeline(context, previewX, previewY + previewHeight + 35, previewWidth, TIMELINE_HEIGHT);
    }

    private void renderTimeline(DrawContext context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, 0xFF333333);

        if (!frames.isEmpty()) {
            float frameWidth = (float) width / frames.size();

            for (int i = 0; i < frames.size(); i++) {
                int fx = x + (int) (i * frameWidth);
                int fWidth = Math.max(1, (int) frameWidth);

                int color = 0xFF555555;

                if (selectionStart >= 0 && selectionEnd >= 0) {
                    int selMin = Math.min(selectionStart, selectionEnd);
                    int selMax = Math.max(selectionStart, selectionEnd);
                    if (i >= selMin && i <= selMax) {
                        color = 0xFF4A90E2;
                    }
                }

                if (i == currentFrame) {
                    color = 0xFFFFFFFF;
                }

                context.fill(fx, y, fx + fWidth, y + height, color);
            }

            String timeLabel = String.format("%s / %s",
                    formatDuration(currentFrame),
                    formatDuration(frames.size()));
            context.drawCenteredTextWithShadow(this.textRenderer, timeLabel,
                    x + width / 2, y - 12, 0xFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (frames.isEmpty()) return super.mouseClicked(mouseX, mouseY, button);

        int timelineY = this.height - 100;
        if (mouseY >= timelineY && mouseY <= timelineY + TIMELINE_HEIGHT) {
            int timelineX = (this.width - Math.min(this.width - 40, 640)) / 2;
            int timelineWidth = Math.min(this.width - 40, 640);

            int clickedFrame = getHoveredFrame(mouseX, timelineX, timelineWidth);

            if (button == 0) {
                currentFrame = MathHelper.clamp(clickedFrame, 0, frames.size() - 1);
                playing = false;
                selectionStart = -1;
                selectionEnd = -1;
                return true;
            } else if (button == 1) {
                if (!selecting) {
                    selectionStart = clickedFrame;
                    selecting = true;
                } else {
                    selectionEnd = clickedFrame;
                    selecting = false;
                    if (selectionStart > selectionEnd) {
                        int temp = selectionStart;
                        selectionStart = selectionEnd;
                        selectionEnd = temp;
                    }
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int getHoveredFrame(double mouseX, int timelineX, int timelineWidth) {
        float frameWidth = (float) timelineWidth / frames.size();
        int frame = (int) ((mouseX - timelineX) / frameWidth);
        return MathHelper.clamp(frame, 0, frames.size() - 1);
    }

    private void togglePlayback() {
        playing = !playing;
        lastFrameTime = System.currentTimeMillis();
    }

    private void cutSelection() {
        if (selectionStart < 0 || selectionEnd < 0 || frames.isEmpty()) return;

        int start = Math.max(0, selectionStart);
        int end = Math.min(frames.size() - 1, selectionEnd);

        List<RecordingFrame> cutFrames = new ArrayList<>(frames.subList(start, end + 1));
        frames.subList(start, end + 1).clear();

        currentFrame = MathHelper.clamp(currentFrame, 0, Math.max(0, frames.size() - 1));
        selectionStart = -1;
        selectionEnd = -1;

        ReplayRecMod.LOGGER.info("Cut {} frames", cutFrames.size());
    }

    private void exportVideo() {
        if (frames.isEmpty()) return;

        RenderSettings settings = new RenderSettings();
        settings.fps = 60;
        settings.quality = 80;

        String outputPath = "recordings/export_" + System.currentTimeMillis() + ".mp4";
        VideoRenderer.getInstance().renderVideo(frames, outputPath, settings);
    }

    private String formatDuration(int ticks) {
        int seconds = ticks / 20;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    public boolean shouldPause() { return false; }
}
