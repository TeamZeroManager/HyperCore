package com.teamzeromanager.hypercore.client.optimization;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Reversible, user-controlled preset that reduces client rendering workload.
 * It does not read GPU temperature or claim to control hardware power directly.
 */
public final class GpuSaver {
    private static final int TARGET_FPS = 45;
    private static final int TARGET_VIEW_DISTANCE = 8;
    private static final double TARGET_ENTITY_DISTANCE = 0.75D;

    private static KeyBinding toggleKey;
    private static boolean enabled;
    private static Integer previousFps;
    private static Integer previousViewDistance;
    private static Double previousEntityDistance;

    private GpuSaver() {
    }

    public static void register() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hypercore.toggle_gpu_saver",
                GLFW.GLFW_KEY_F7,
                "key.categories.hypercore"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                toggle(client);
            }
        });
    }

    private static void toggle(MinecraftClient client) {
        if (client.options == null) {
            return;
        }

        if (enabled) {
            restore(client);
        } else {
            apply(client);
        }
    }

    private static void apply(MinecraftClient client) {
        SimpleOption<Integer> fps = client.options.getMaxFps();
        SimpleOption<Integer> viewDistance = client.options.getViewDistance();
        SimpleOption<Double> entityDistance = client.options.getEntityDistanceScaling();

        previousFps = fps.getValue();
        previousViewDistance = viewDistance.getValue();
        previousEntityDistance = entityDistance.getValue();

        fps.setValue(Math.min(previousFps, TARGET_FPS));
        viewDistance.setValue(Math.min(previousViewDistance, TARGET_VIEW_DISTANCE));
        entityDistance.setValue(Math.min(previousEntityDistance, TARGET_ENTITY_DISTANCE));

        enabled = true;
        client.options.write();
        client.inGameHud.getChatHud().addMessage(Text.translatable("message.hypercore.gpu_saver_enabled"));
    }

    private static void restore(MinecraftClient client) {
        if (previousFps != null) {
            client.options.getMaxFps().setValue(previousFps);
        }
        if (previousViewDistance != null) {
            client.options.getViewDistance().setValue(previousViewDistance);
        }
        if (previousEntityDistance != null) {
            client.options.getEntityDistanceScaling().setValue(previousEntityDistance);
        }

        enabled = false;
        client.options.write();
        client.inGameHud.getChatHud().addMessage(Text.translatable("message.hypercore.gpu_saver_disabled"));
        previousFps = null;
        previousViewDistance = null;
        previousEntityDistance = null;
    }

    public static boolean isEnabled() {
        return enabled;
    }
}
