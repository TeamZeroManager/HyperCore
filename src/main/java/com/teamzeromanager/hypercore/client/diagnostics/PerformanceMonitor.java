package com.teamzeromanager.hypercore.client.diagnostics;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Collects lightweight client performance samples.
 *
 * Sampling is deliberately performed once every 20 client ticks rather than
 * every rendered frame. This keeps diagnostics overhead predictable.
 */
public final class PerformanceMonitor {
    public static final int SAMPLE_INTERVAL_TICKS = 20;
    public static final int MAX_HISTORY_SIZE = 60;

    private static final PerformanceMonitor INSTANCE = new PerformanceMonitor();

    private final Deque<PerformanceSnapshot> history = new ArrayDeque<>(MAX_HISTORY_SIZE);
    private int ticksSinceLastSample;

    private PerformanceMonitor() {
    }

    public static PerformanceMonitor getInstance() {
        return INSTANCE;
    }

    /**
     * Registers the monitor once during client initialization.
     */
    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> INSTANCE.onClientTick(client));
    }

    private void onClientTick(MinecraftClient client) {
        ticksSinceLastSample++;
        if (ticksSinceLastSample < SAMPLE_INTERVAL_TICKS) {
            return;
        }

        ticksSinceLastSample = 0;
        collectSample(client);
    }

    private void collectSample(MinecraftClient client) {
        Runtime runtime = Runtime.getRuntime();
        long committedHeap = runtime.totalMemory();
        long freeHeap = runtime.freeMemory();
        long usedHeap = committedHeap - freeHeap;

        PerformanceSnapshot snapshot = new PerformanceSnapshot(
                System.nanoTime(),
                client.getCurrentFps(),
                usedHeap,
                committedHeap,
                runtime.maxMemory(),
                runtime.availableProcessors(),
                System.getProperty("java.version", "unknown"),
                System.getProperty("os.name", "unknown"),
                System.getProperty("os.version", "unknown")
        );

        synchronized (history) {
            if (history.size() == MAX_HISTORY_SIZE) {
                history.removeFirst();
            }
            history.addLast(snapshot);
        }
    }

    /**
     * Returns the most recent sample, or null until the first sample exists.
     */
    public PerformanceSnapshot getLatestSnapshot() {
        synchronized (history) {
            return history.peekLast();
        }
    }

    /**
     * Returns a stable copy of the sample history, oldest first.
     */
    public List<PerformanceSnapshot> getHistory() {
        synchronized (history) {
            return List.copyOf(new ArrayList<>(history));
        }
    }

    public boolean hasSamples() {
        synchronized (history) {
            return !history.isEmpty();
        }
    }
}
