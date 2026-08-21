package com.replayrec.keybinds;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    public static KeyBinding TOGGLE_RECORDING;
    public static KeyBinding OPEN_EDITOR;
    public static KeyBinding QUICK_SAVE;
    public static KeyBinding TOGGLE_AUDIO;
    public static KeyBinding PLAY_PAUSE;
    public static KeyBinding REWIND_5S;
    public static KeyBinding FORWARD_5S;

    public static void register() {
        TOGGLE_RECORDING = registerKey("key.replayrec.toggle_recording", GLFW.GLFW_KEY_R);
        OPEN_EDITOR = registerKey("key.replayrec.open_editor", GLFW.GLFW_KEY_E);
        QUICK_SAVE = registerKey("key.replayrec.quick_save", GLFW.GLFW_KEY_F9);
        TOGGLE_AUDIO = registerKey("key.replayrec.toggle_audio", GLFW.GLFW_KEY_F10);
        PLAY_PAUSE = registerKey("key.replayrec.play_pause", GLFW.GLFW_KEY_SPACE);
        REWIND_5S = registerKey("key.replayrec.rewind_5s", GLFW.GLFW_KEY_LEFT);
        FORWARD_5S = registerKey("key.replayrec.forward_5s", GLFW.GLFW_KEY_RIGHT);
    }

    private static KeyBinding registerKey(String translationKey, int defaultKey) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(
                translationKey,
                InputUtil.Type.KEYSYM,
                defaultKey,
                "category.replayrec"
        ));
    }
}
