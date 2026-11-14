package com.teamzeromanager.hypercore.client.diagnostics;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.DrawContext;

import java.util.Arrays;

/**
 * Measures intervals between HUD render callbacks as an approximation of
 * displayed frame times. These are CPU-side frame intervals, not GPU timings.
 *
 * All state is accessed on the Minecraft render thread.
 */
public final class FrameTimeMonitor {
    public static final int HISTORY_SIZE = 120;
    private static final long STATISTICS_REFRESH_NANOS = 250_000_000L;

    private static final FrameTimeMonitor INSTANCE = new FrameTimeMonitor();

    private final double[] samples = new double[HISTORY_SIZE];
    private final double[] sortedSamples = new double[HISTORY_SIZE];
    private int sampleCount;
    private int nextIndex;
    private long previousFrameNanos;
    private long lastStatisticsUpdateNanos;
    private double latestFrameTimeMs;
    private double averageFrameTimeMs;
    private double percentile95Ms;

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
                latestFrameTimeMs = frameTimeMs;
                samples[nextIndex] = frameTimeMs;
                nextIndex = (nextIndex + 1) % HISTORY_SIZE;
                if (sampleCount < HISTORY_SIZE) {
                    sampleCount++;
                }

                if (now - lastStatisticsUpdateNanos >= STATISTICS_REFRESH_NANOS) {
                    refreshStatistics(now);
                }
            }
        }
        previousFrameNanos = now;
    }

    private void refreshStatistics(long now) {
        if (sampleCount == 0) {
            return;
        }

        double total = 0.0;
        for (int i = 0; i < sampleCount; i++) {
            total += samples[i];
        }
        averageFrameTimeMs = total / sampleCount;

        System.arraycopy(samples, 0, sortedSamples, 0, sampleCount);
        Arrays.sort(sortedSamples, 0, sampleCount);
        int percentileIndex = Math.max(0, (int) Math.ceil(sampleCount * 0.95) - 1);
        percentile95Ms = sortedSamples[percentileIndex];
        lastStatisticsUpdateNanos = now;
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public double getLatestFrameTimeMs() {
        return latestFrameTimeMs;
    }

    public double getAverageFrameTimeMs() {
        return averageFrameTimeMs;
    }

    /**
     * Returns the 95th percentile interval in the rolling sample window.
     * Higher values indicate slower frames in that window.
     */
    public double get95thPercentileMs() {
        return percentile95Ms;
    }
}
