package com.replayrec.mixin;

import com.replayrec.recording.RecordingManager;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void onFrameRendered(net.minecraft.client.render.tick_counter.TickCounter tickCounter, boolean tick, CallbackInfo ci) {
        RecordingManager manager = RecordingManager.getInstance();
        if (manager.isRecording() && !manager.isPaused()) {
            manager.enqueueFrame(() -> {});
        }
    }
}
