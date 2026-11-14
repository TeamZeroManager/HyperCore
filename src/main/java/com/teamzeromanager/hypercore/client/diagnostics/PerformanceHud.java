package com.teamzeromanager.hypercore.client.diagnostics;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Small, opt-in on-screen view of the latest diagnostic sample.
 * Text and layout are cached on client ticks so rendering the HUD does not
 * allocate and format a new set of strings every rendered frame.
 */
public final class PerformanceHud {
    private static final int LEFT = 8;
    private static final int TOP = 8;
    private static final int LINE_HEIGHT = 12;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int PANEL_COLOR = 0x99000000;
    private static final int REFRESH_INTERVAL_TICKS = 10;

    private static boolean visible;
    private static KeyBinding toggleKey;
    private static int ticksSinceRefresh;
    private static Text[] cachedTexts = {Text.literal("HyperCore"), Text.literal("Collecting first sample...")};
    private static int cachedWidth;
    private static int cachedHeight;

    private PerformanceHud() {
    }

    public static void register() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hypercore.toggle_hud",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F8,
                "key.categories.hypercore"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                visible = !visible;
                if (visible) {
                    refreshDisplay(client);
                }
            }

            if (visible && ++ticksSinceRefresh >= REFRESH_INTERVAL_TICKS) {
                ticksSinceRefresh = 0;
                refreshDisplay(client);
            }
        });

        HudRenderCallback.EVENT.register(PerformanceHud::render);
    }

    private static void refreshDisplay(MinecraftClient client) {
        PerformanceSnapshot snapshot = PerformanceMonitor.getInstance().getLatestSnapshot();
        FrameTimeMonitor frameTimes = FrameTimeMonitor.getInstance();

        if (snapshot == null) {
            String[] lines = {"HyperCore", "Collecting first sample..."};
            cacheLines(client, lines);
        } else {
            String[] lines = {
                    "HyperCore Diagnostics",
                    "FPS: " + snapshot.framesPerSecond(),
                    String.format(java.util.Locale.ROOT, "Frame time: %.2f ms",
                            frameTimes.getLatestFrameTimeMs()),
                    String.format(java.util.Locale.ROOT, "Average: %.2f ms | 95th: %.2f ms",
                            frameTimes.getAverageFrameTimeMs(), frameTimes.get95thPercentileMs()),
                    String.format(java.util.Locale.ROOT, "Memory: %.0f / %.0f MiB",
                            snapshot.usedHeapMiB(), snapshot.maximumHeapMiB()),
                    "Logical processors: " + snapshot.availableProcessors(),
                    "Java: " + snapshot.javaVersion(),
                    "OS: " + snapshot.operatingSystem() + " " + snapshot.operatingSystemVersion()
            };
            cacheLines(client, lines);
        }
    }

    private static void cacheLines(MinecraftClient client, String[] lines) {
        cachedTexts = new Text[lines.length];
        int width = 0;
        for (int i = 0; i < lines.length; i++) {
            cachedTexts[i] = Text.literal(lines[i]);
            width = Math.max(width, client.textRenderer.getWidth(lines[i]));
        }
        cachedWidth = width + 12;
        cachedHeight = cachedTexts.length * LINE_HEIGHT + 8;
    }

    private static void render(DrawContext context, float tickDelta) {
        if (!visible) {
            return;
        }

        context.fill(LEFT, TOP, LEFT + cachedWidth, TOP + cachedHeight, PANEL_COLOR);
        MinecraftClient client = MinecraftClient.getInstance();

        for (int i = 0; i < cachedTexts.length; i++) {
            context.drawTextWithShadow(
                    client.textRenderer,
                    cachedTexts[i],
                    LEFT + 6,
                    TOP + 4 + i * LINE_HEIGHT,
                    TEXT_COLOR
            );
        }
    }
}
