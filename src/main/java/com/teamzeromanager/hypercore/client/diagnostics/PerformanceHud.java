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
 * Rendering reads the cached snapshot; it does not collect new metrics.
 */
public final class PerformanceHud {
    private static final int LEFT = 8;
    private static final int TOP = 8;
    private static final int LINE_HEIGHT = 12;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int PANEL_COLOR = 0x99000000;

    private static boolean visible;
    private static KeyBinding toggleKey;

    private PerformanceHud() {
    }

    public static void register() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hypercore.toggle_hud",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F8,
                "category.hypercore"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                visible = !visible;
            }
        });

        HudRenderCallback.EVENT.register(PerformanceHud::render);
    }

    private static void render(DrawContext context, float tickDelta) {
        if (!visible) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        PerformanceSnapshot snapshot = PerformanceMonitor.getInstance().getLatestSnapshot();
        if (snapshot == null) {
            drawLines(context, client, new String[] {
                    "HyperCore",
                    "Collecting first sample..."
            });
            return;
        }

        String[] lines = {
                "HyperCore Diagnostics",
                "FPS: " + snapshot.framesPerSecond(),
                String.format(
                        java.util.Locale.ROOT,
                        "Memory: %.0f / %.0f MiB",
                        snapshot.usedHeapMiB(),
                        snapshot.maximumHeapMiB()
                ),
                "Logical processors: " + snapshot.availableProcessors(),
                "Java: " + snapshot.javaVersion(),
                "OS: " + snapshot.operatingSystem() + " " + snapshot.operatingSystemVersion()
        };
        drawLines(context, client, lines);
    }

    private static void drawLines(DrawContext context, MinecraftClient client, String[] lines) {
        int width = 0;
        for (String line : lines) {
            width = Math.max(width, client.textRenderer.getWidth(line));
        }

        int panelWidth = width + 12;
        int panelHeight = lines.length * LINE_HEIGHT + 8;
        context.fill(LEFT, TOP, LEFT + panelWidth, TOP + panelHeight, PANEL_COLOR);

        for (int i = 0; i < lines.length; i++) {
            context.drawTextWithShadow(
                    client.textRenderer,
                    Text.literal(lines[i]),
                    LEFT + 6,
                    TOP + 4 + i * LINE_HEIGHT,
                    TEXT_COLOR
            );
        }
    }
}
