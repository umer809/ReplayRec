package com.replayrec.mixin;

import com.replayrec.recording.RecordingManager;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Inject(method = "run", at = @At("RETURN"))
    private void onGameClose(CallbackInfo ci) {
        RecordingManager manager = RecordingManager.getInstance();
        if (manager.isRecording()) {
            manager.stopRecording();
        }
    }
}
