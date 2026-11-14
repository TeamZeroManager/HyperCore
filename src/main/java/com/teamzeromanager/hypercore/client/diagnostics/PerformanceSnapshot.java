package com.teamzeromanager.hypercore.client.diagnostics;

/**
 * Immutable point-in-time sample of client performance and runtime memory.
 *
 * All memory values are expressed in bytes. The timestamp is based on
 * System.nanoTime() and is intended for ordering samples, not wall-clock display.
 */
public record PerformanceSnapshot(
        long timestampNanos,
        int framesPerSecond,
        long usedHeapBytes,
        long committedHeapBytes,
        long maximumHeapBytes,
        int availableProcessors,
        String javaVersion,
        String operatingSystem,
        String operatingSystemVersion
) {
    public double usedHeapMiB() {
        return bytesToMiB(usedHeapBytes);
    }

    public double committedHeapMiB() {
        return bytesToMiB(committedHeapBytes);
    }

    public double maximumHeapMiB() {
        return bytesToMiB(maximumHeapBytes);
    }

    private static double bytesToMiB(long bytes) {
        return bytes / (1024.0 * 1024.0);
    }
}
