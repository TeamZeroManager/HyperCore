package com.teamzeromanager.hypercore.client.diagnostics;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.DrawContext;

import java.util.Arrays;

/**
 * Measures time between HUD render callbacks as an approximation of displayed
 * frame intervals. Values are in milliseconds and are not GPU-only timings.
 */
public final class FrameTimeMonitor {
    public static final int HISTORY_SIZE = 120;

    private static final FrameTimeMonitor INSTANCE = new FrameTimeMonitor();

    private final double[] samples = new double[HISTORY_SIZE];
    private int sampleCount;
    private int nextIndex;
    private long previousFrameNanos;

    private FrameTimeMonitor() {
    }

    public static FrameTimeMonitor getInstance() {
        return INSTANCE;
    }

    public static void register() {
        HudRenderCallback.EVENT.register((DrawContext context, float tickDelta) ->
                INSTANCE.recordFrame());
    }

    private void recordFrame() {
        long now = System.nanoTime();
        if (previousFrameNanos != 0L) {
            double frameTimeMs = (now - previousFrameNanos) / 1_000_000.0;
            if (frameTimeMs > 0.0 && Double.isFinite(frameTimeMs)) {
                samples[nextIndex] = frameTimeMs;
                nextIndex = (nextIndex + 1) % HISTORY_SIZE;
                if (sampleCount < HISTORY_SIZE) {
                    sampleCount++;
                }
            }
        }
        previousFrameNanos = now;
    }

    public synchronized int getSampleCount() {
        return sampleCount;
    }

    public synchronized double getLatestFrameTimeMs() {
        if (sampleCount == 0) {
            return 0.0;
        }
        int latestIndex = (nextIndex - 1 + HISTORY_SIZE) % HISTORY_SIZE;
        return samples[latestIndex];
    }

    public synchronized double getAverageFrameTimeMs() {
        if (sampleCount == 0) {
            return 0.0;
        }
        double total = 0.0;
        for (int i = 0; i < sampleCount; i++) {
            total += samples[i];
        }
        return total / sampleCount;
    }

    /**
     * Returns the 95th percentile frame interval from the rolling window.
     * A higher value indicates slower frames in the measured window.
     */
    public synchronized double get95thPercentileMs() {
        if (sampleCount == 0) {
            return 0.0;
        }
        double[] sorted = Arrays.copyOf(samples, sampleCount);
        Arrays.sort(sorted);
        int index = Math.max(0, (int) Math.ceil(sampleCount * 0.95) - 1);
        return sorted[index];
    }
}
