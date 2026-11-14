# HyperCore diagnostics

## Current implementation

The first diagnostics module samples the Minecraft client once every 20 client ticks.

Each sample records:

- Current client FPS reported by Minecraft.
- JVM heap usage (used, committed, and maximum).
- Number of processors visible to the JVM.
- Java version.
- Operating system name and version.
- A monotonic timestamp for ordering samples.

The monitor retains at most 60 samples in memory. Oldest samples are discarded when the history is full.

## Important measurement notes

- Heap usage is JVM heap usage, not total system RAM usage.
- The processor count is the number of processors reported by the JVM; it is not a CPU utilization percentage.
- FPS is the value reported by Minecraft at the sampling moment, not a frame-time benchmark.
- Samples are collected on the client tick event, not on every rendered frame.
- The monitor does not change graphics settings, garbage-collect, or modify world data.

## Next diagnostics milestones

1. Add a user-facing diagnostics screen or HUD.
2. Add frame-time statistics using a carefully validated sampling mechanism.
3. Add optional export of diagnostic snapshots for reproducible comparisons.
4. Establish baseline test scenarios before implementing automatic optimizations.
