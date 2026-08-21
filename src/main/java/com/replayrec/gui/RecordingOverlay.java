package com.replayrec.gui;

import com.replayrec.recording.RecordingManager;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class RecordingOverlay {
    private static RecordingOverlay INSTANCE;
    private boolean visible = true;

    public static RecordingOverlay getInstance() {
        if (INSTANCE == null) INSTANCE = new RecordingOverlay();
        return INSTANCE;
    }

    public static void register() {
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            getInstance().render(drawContext);
        });
    }

    public void render(DrawContext context) {
        if (!visible) return;

        RecordingManager manager = RecordingManager.getInstance();
        if (!manager.isRecording()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;

        int x = 10;
        int y = 10;
        int lineHeight = textRenderer.fontHeight + 2;

        context.fill(x - 2, y - 2, x + 120, y + lineHeight * 3 + 4, 0x80000000);

        String status = "● REC";
        context.drawTextWithShadow(
                textRenderer,
                Text.literal(status).formatted(Formatting.RED),
                x, y, 0xFFFFFF
        );

        long duration = manager.getRecordingDurationMs() / 1000;
        String time = String.format("%02d:%02d:%02d",
                duration / 3600,
                (duration % 3600) / 60,
                duration % 60);
        context.drawTextWithShadow(textRenderer, time, x, y + lineHeight, 0xFFFFFF);

        String frames = manager.getFrameCount() + " frames";
        context.drawTextWithShadow(textRenderer, frames, x, y + lineHeight * 2, 0xAAAAAA);
    }

    public void toggle() { visible = !visible; }
}
