package com.replayrec;

import com.replayrec.config.ModConfig;
import com.replayrec.gui.RecordingOverlay;
import com.replayrec.gui.SettingsScreen;
import com.replayrec.recording.RecordingManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReplayRecMod implements ClientModInitializer {
    public static final String MOD_ID = "replayrec";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyBinding recordKey;
    private static KeyBinding pauseKey;
    private static KeyBinding overlayKey;
    private static KeyBinding settingsKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("ReplayRec initializing...");

        ModConfig.getInstance().load();
        RecordingManager.getInstance();
        RecordingOverlay.register();

        setupKeybindings();

        LOGGER.info("ReplayRec initialized!");
    }

    private void setupKeybindings() {
        recordKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.replayrec.record",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F9,
                "category.replayrec"
        ));

        pauseKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.replayrec.pause",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F10,
                "category.replayrec"
        ));

        overlayKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.replayrec.overlay",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F11,
                "category.replayrec"
        ));

        settingsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.replayrec.settings",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F12,
                "category.replayrec"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (recordKey.wasPressed()) {
                RecordingManager manager = RecordingManager.getInstance();
                if (manager.isRecording()) {
                    manager.stopRecording();
                } else {
                    manager.startRecording();
                }
            }
            if (pauseKey.wasPressed()) {
                RecordingManager manager = RecordingManager.getInstance();
                if (manager.isRecording()) {
                    manager.togglePause();
                }
            }
            if (overlayKey.wasPressed()) {
                RecordingOverlay.getInstance().toggle();
            }
            if (settingsKey.wasPressed()) {
                MinecraftClient.getInstance().setScreen(
                    new SettingsScreen(MinecraftClient.getInstance().currentScreen)
                );
            }
        });
    }
}
