package com.replayrec;

import com.replayrec.config.ModConfig;
import com.replayrec.gui.RecordingOverlay;
import com.replayrec.gui.TimelineScreen;
import com.replayrec.keybinds.KeyBindings;
import com.replayrec.recording.AudioRecorder;
import com.replayrec.recording.RecordingManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReplayRecMod implements ClientModInitializer {
    public static final String MOD_ID = "replayrec";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("ReplayRec initializing...");

        ModConfig.getInstance().load();
        KeyBindings.register();
        RecordingOverlay.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (KeyBindings.TOGGLE_RECORDING.wasPressed()) {
                RecordingManager manager = RecordingManager.getInstance();
                if (manager.isRecording()) {
                    manager.stopRecording();
                    AudioRecorder.getInstance().stopRecording();
                } else {
                    manager.startRecording();
                    AudioRecorder.getInstance().startRecording(ModConfig.getInstance().recordMicrophone);
                }
            }

            if (KeyBindings.OPEN_EDITOR.wasPressed()) {
                if (!RecordingManager.getInstance().getAllFrames().isEmpty()) {
                    client.setScreen(new TimelineScreen());
                }
            }

            if (KeyBindings.QUICK_SAVE.wasPressed()) {
                RecordingManager.getInstance().quickSave();
            }

            if (KeyBindings.TOGGLE_AUDIO.wasPressed()) {
                ModConfig config = ModConfig.getInstance();
                config.recordAudio = !config.recordAudio;
                config.save();
            }
        });

        LOGGER.info("ReplayRec initialized!");
    }
}
