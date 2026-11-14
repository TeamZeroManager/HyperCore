package com.teamzeromanager.hypercore.client.optimization;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
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

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/**
 * Reversible, user-controlled preset that reduces client rendering workload.
 * A durable backup allows the original settings to be recovered after a crash
 * or an ordinary restart while the preset is active.
 *
 * This class does not read GPU temperature or control hardware power directly.
 */
public final class GpuSaver {
    private static final int TARGET_FPS = 50;
    private static final int TARGET_VIEW_DISTANCE = 8;
    private static final double TARGET_ENTITY_DISTANCE = 0.75D;
    private static final String BACKUP_FILE = "gpu-saver-backup.properties";

    private static KeyBinding toggleKey;
    private static boolean enabled;
    private static boolean recoveryAttempted;
    private static boolean recoveryBlocked;

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
            if (!recoveryAttempted && client.options != null) {
                recoveryAttempted = true;
                recoveryBlocked = !recoverInterruptedPreset(client);
            }

            while (toggleKey.wasPressed()) {
                if (recoveryBlocked) {
                    client.inGameHud.getChatHud().addMessage(
                            Text.translatable("message.hypercore.gpu_saver_recovery_failed"));
                } else {
                    toggle(client);
                }
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

        SettingsSnapshot snapshot = new SettingsSnapshot(
                fps.getValue(),
                viewDistance.getValue(),
                entityDistance.getValue(),
                options.getCloudRenderMode().getValue(),
                options.getGraphicsMode().getValue(),
                options.getParticles().getValue(),
                options.getAo().getValue(),
                options.getEntityShadows().getValue(),
                options.getBiomeBlendRadius().getValue()
        );

        // Persist before touching GameOptions so an interrupted apply is recoverable.
        if (!saveSnapshot(snapshot)) {
            client.inGameHud.getChatHud().addMessage(
                    Text.translatable("message.hypercore.gpu_saver_backup_failed"));
            return;
        }

        remember(snapshot);

        // Never raise a lower user-selected cap; 50 FPS is the saver-mode ceiling.
        fps.setValue(Math.min(snapshot.fps(), TARGET_FPS));
        viewDistance.setValue(Math.min(snapshot.viewDistance(), TARGET_VIEW_DISTANCE));
        entityDistance.setValue(Math.min(snapshot.entityDistance(), TARGET_ENTITY_DISTANCE));
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
        SettingsSnapshot snapshot = currentSnapshot();
        restoreMatchingPresetValues(client.options, snapshot);
        client.options.write();

        enabled = false;
        clearBackup();
        clearRememberedSettings();
        client.inGameHud.getChatHud().addMessage(Text.translatable("message.hypercore.gpu_saver_disabled"));
    }

    /**
     * If the game ended while the preset was active, recover it on the first
     * client tick after options have loaded. Manual changes are preserved.
     */
    private static boolean recoverInterruptedPreset(MinecraftClient client) {
        Path backup = getBackupPath();
        if (!Files.exists(backup)) {
            return true;
        }

        try (Reader reader = Files.newBufferedReader(backup)) {
            Properties properties = new Properties();
            properties.load(reader);
            SettingsSnapshot snapshot = SettingsSnapshot.from(properties);
            restoreMatchingPresetValues(client.options, snapshot);
            client.options.write();

            if (!deleteBackup()) {
                System.err.println("[HyperCore] Recovered GPU saver settings, but could not remove the backup file.");
                return false;
            }

            client.inGameHud.getChatHud().addMessage(
                    Text.translatable("message.hypercore.gpu_saver_recovered"));
            return true;
        } catch (IOException | IllegalArgumentException exception) {
            System.err.println("[HyperCore] Could not recover GPU saver settings: " + exception.getMessage());
            return false;
        }
    }

    private static void restoreMatchingPresetValues(GameOptions options, SettingsSnapshot snapshot) {
        // Restore only values that still match the preset. If the player changed
        // an option while saver mode was active, keep that newer choice.
        if (options.getMaxFps().getValue() == Math.min(snapshot.fps(), TARGET_FPS)) {
            options.getMaxFps().setValue(snapshot.fps());
        }
        if (options.getViewDistance().getValue() == Math.min(snapshot.viewDistance(), TARGET_VIEW_DISTANCE)) {
            options.getViewDistance().setValue(snapshot.viewDistance());
        }
        if (Double.compare(options.getEntityDistanceScaling().getValue(),
                Math.min(snapshot.entityDistance(), TARGET_ENTITY_DISTANCE)) == 0) {
            options.getEntityDistanceScaling().setValue(snapshot.entityDistance());
        }
        if (options.getCloudRenderMode().getValue() == CloudRenderMode.OFF) {
            options.getCloudRenderMode().setValue(snapshot.cloudMode());
        }
        if (options.getGraphicsMode().getValue() == GraphicsMode.FAST) {
            options.getGraphicsMode().setValue(snapshot.graphicsMode());
        }
        if (options.getParticles().getValue() == ParticlesMode.MINIMAL) {
            options.getParticles().setValue(snapshot.particlesMode());
        }
        if (!options.getAo().getValue()) {
            options.getAo().setValue(snapshot.ambientOcclusion());
        }
        if (!options.getEntityShadows().getValue()) {
            options.getEntityShadows().setValue(snapshot.entityShadows());
        }
        if (options.getBiomeBlendRadius().getValue() == 0) {
            options.getBiomeBlendRadius().setValue(snapshot.biomeBlend());
        }
    }

    private static void remember(SettingsSnapshot snapshot) {
        previousFps = snapshot.fps();
        previousViewDistance = snapshot.viewDistance();
        previousEntityDistance = snapshot.entityDistance();
        previousCloudMode = snapshot.cloudMode();
        previousGraphicsMode = snapshot.graphicsMode();
        previousParticlesMode = snapshot.particlesMode();
        previousAmbientOcclusion = snapshot.ambientOcclusion();
        previousEntityShadows = snapshot.entityShadows();
        previousBiomeBlend = snapshot.biomeBlend();
    }

    private static SettingsSnapshot currentSnapshot() {
        return new SettingsSnapshot(
                previousFps,
                previousViewDistance,
                previousEntityDistance,
                previousCloudMode,
                previousGraphicsMode,
                previousParticlesMode,
                previousAmbientOcclusion,
                previousEntityShadows,
                previousBiomeBlend
        );
    }

    private static boolean saveSnapshot(SettingsSnapshot snapshot) {
        Path backup = getBackupPath();
        Path temporary = backup.resolveSibling(BACKUP_FILE + ".tmp");
        Properties properties = snapshot.toProperties();

        try {
            Files.createDirectories(backup.getParent());
            try (Writer writer = Files.newBufferedWriter(temporary)) {
                properties.store(writer, "HyperCore GPU saver recovery data");
            }
            try {
                Files.move(temporary, backup, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, backup, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException exception) {
            System.err.println("[HyperCore] Could not save GPU saver recovery data: " + exception.getMessage());
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException ignored) {
                // Keep the original persistence error.
            }
            return false;
        }
    }

    private static Path getBackupPath() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("hypercore")
                .resolve(BACKUP_FILE);
    }

    private static void clearBackup() {
        if (!deleteBackup()) {
            System.err.println("[HyperCore] Could not remove GPU saver recovery data.");
        }
    }

    private static boolean deleteBackup() {
        try {
            Files.deleteIfExists(getBackupPath());
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    private static void clearRememberedSettings() {
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

    private record SettingsSnapshot(
            int fps,
            int viewDistance,
            double entityDistance,
            CloudRenderMode cloudMode,
            GraphicsMode graphicsMode,
            ParticlesMode particlesMode,
            boolean ambientOcclusion,
            boolean entityShadows,
            int biomeBlend
    ) {
        private Properties toProperties() {
            Properties properties = new Properties();
            properties.setProperty("fps", Integer.toString(fps));
            properties.setProperty("viewDistance", Integer.toString(viewDistance));
            properties.setProperty("entityDistance", Double.toString(entityDistance));
            properties.setProperty("cloudMode", cloudMode.name());
            properties.setProperty("graphicsMode", graphicsMode.name());
            properties.setProperty("particlesMode", particlesMode.name());
            properties.setProperty("ambientOcclusion", Boolean.toString(ambientOcclusion));
            properties.setProperty("entityShadows", Boolean.toString(entityShadows));
            properties.setProperty("biomeBlend", Integer.toString(biomeBlend));
            return properties;
        }

        private static SettingsSnapshot from(Properties properties) {
            return new SettingsSnapshot(
                    Integer.parseInt(required(properties, "fps")),
                    Integer.parseInt(required(properties, "viewDistance")),
                    Double.parseDouble(required(properties, "entityDistance")),
                    CloudRenderMode.valueOf(required(properties, "cloudMode")),
                    GraphicsMode.valueOf(required(properties, "graphicsMode")),
                    ParticlesMode.valueOf(required(properties, "particlesMode")),
                    Boolean.parseBoolean(required(properties, "ambientOcclusion")),
                    Boolean.parseBoolean(required(properties, "entityShadows")),
                    Integer.parseInt(required(properties, "biomeBlend"))
            );
        }

        private static String required(Properties properties, String key) {
            String value = properties.getProperty(key);
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Missing recovery property: " + key);
            }
            return value;
        }
    }
}
