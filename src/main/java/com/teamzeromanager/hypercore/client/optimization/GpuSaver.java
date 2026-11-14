package com.teamzeromanager.hypercore.client.optimization;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.ParticlesMode;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Reversible, user-controlled preset that reduces client rendering workload.
 * It does not read GPU temperature or control hardware power directly.
 */
public final class GpuSaver {
    private static final int TARGET_FPS = 50;
    private static final int TARGET_VIEW_DISTANCE = 8;
    private static final double TARGET_ENTITY_DISTANCE = 0.75D;

    private static KeyBinding toggleKey;
    private static boolean enabled;

    private static Integer previousFps;
    private static Integer previousViewDistance;
    private static Double previousEntityDistance;
    private static CloudRenderMode previousCloudMode;
    private static GraphicsMode previousGraphicsMode;
    private static ParticlesMode previousParticlesMode;
    private static Boolean previousAmbientOcclusion;
    private static Boolean previousEntityShadows;
    private static Integer previousBiomeBlend;

    private GpuSaver() {
    }

    public static void register() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hypercore.toggle_gpu_saver",
                InputUtil.Type.KEYSYM,
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
        GameOptions options = client.options;
        SimpleOption<Integer> fps = options.getMaxFps();
        SimpleOption<Integer> viewDistance = options.getViewDistance();
        SimpleOption<Double> entityDistance = options.getEntityDistanceScaling();

        previousFps = fps.getValue();
        previousViewDistance = viewDistance.getValue();
        previousEntityDistance = entityDistance.getValue();
        previousCloudMode = options.getCloudRenderMode().getValue();
        previousGraphicsMode = options.getGraphicsMode().getValue();
        previousParticlesMode = options.getParticles().getValue();
        previousAmbientOcclusion = options.getAo().getValue();
        previousEntityShadows = options.getEntityShadows().getValue();
        previousBiomeBlend = options.getBiomeBlendRadius().getValue();

        // Never raise a lower user-selected cap; 50 FPS is the saver-mode ceiling.
        fps.setValue(Math.min(previousFps, TARGET_FPS));
        viewDistance.setValue(Math.min(previousViewDistance, TARGET_VIEW_DISTANCE));
        entityDistance.setValue(Math.min(previousEntityDistance, TARGET_ENTITY_DISTANCE));
        options.getCloudRenderMode().setValue(CloudRenderMode.OFF);
        options.getGraphicsMode().setValue(GraphicsMode.FAST);
        options.getParticles().setValue(ParticlesMode.MINIMAL);
        options.getAo().setValue(false);
        options.getEntityShadows().setValue(false);
        options.getBiomeBlendRadius().setValue(0);

        enabled = true;
        options.write();
        client.inGameHud.getChatHud().addMessage(Text.translatable("message.hypercore.gpu_saver_enabled"));
    }

    private static void restore(MinecraftClient client) {
        GameOptions options = client.options;

        // Restore only values that still match the preset. If the player changed
        // an option while saver mode was active, keep that newer choice.
        if (previousFps != null
                && options.getMaxFps().getValue() == Math.min(previousFps, TARGET_FPS)) {
            options.getMaxFps().setValue(previousFps);
        }
        if (previousViewDistance != null
                && options.getViewDistance().getValue() == Math.min(previousViewDistance, TARGET_VIEW_DISTANCE)) {
            options.getViewDistance().setValue(previousViewDistance);
        }
        if (previousEntityDistance != null
                && Double.compare(options.getEntityDistanceScaling().getValue(),
                Math.min(previousEntityDistance, TARGET_ENTITY_DISTANCE)) == 0) {
            options.getEntityDistanceScaling().setValue(previousEntityDistance);
        }
        if (previousCloudMode != null && options.getCloudRenderMode().getValue() == CloudRenderMode.OFF) {
            options.getCloudRenderMode().setValue(previousCloudMode);
        }
        if (previousGraphicsMode != null && options.getGraphicsMode().getValue() == GraphicsMode.FAST) {
            options.getGraphicsMode().setValue(previousGraphicsMode);
        }
        if (previousParticlesMode != null && options.getParticles().getValue() == ParticlesMode.MINIMAL) {
            options.getParticles().setValue(previousParticlesMode);
        }
        if (previousAmbientOcclusion != null && !options.getAo().getValue()) {
            options.getAo().setValue(previousAmbientOcclusion);
        }
        if (previousEntityShadows != null && !options.getEntityShadows().getValue()) {
            options.getEntityShadows().setValue(previousEntityShadows);
        }
        if (previousBiomeBlend != null && options.getBiomeBlendRadius().getValue() == 0) {
            options.getBiomeBlendRadius().setValue(previousBiomeBlend);
        }

        enabled = false;
        options.write();
        client.inGameHud.getChatHud().addMessage(Text.translatable("message.hypercore.gpu_saver_disabled"));

        previousFps = null;
        previousViewDistance = null;
        previousEntityDistance = null;
        previousCloudMode = null;
        previousGraphicsMode = null;
        previousParticlesMode = null;
        previousAmbientOcclusion = null;
        previousEntityShadows = null;
        previousBiomeBlend = null;
    }

    public static boolean isEnabled() {
        return enabled;
    }
}
