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
- FPS and JVM samples are collected on the client tick event, not on every rendered frame.
- Frame time is measured between Fabric HUD render callbacks and stored as a rolling window of up to 120 intervals. It approximates frame pacing from the client render loop; it is not a GPU timer and does not isolate rendering, driver, or display latency.
- The HUD refreshes the average and 95th-percentile frame interval at most four times per second to keep diagnostic overhead low.
- The monitor does not change graphics settings, garbage-collect, or modify world data.

## Current HUD

Press **F8** to show or hide the diagnostics panel. It displays the latest sampled FPS and JVM memory values, basic runtime details, the latest frame interval, the rolling average, and the 95th-percentile frame interval. The rolling frame-time window contains up to 120 intervals.

## Next diagnostics milestones

1. Add optional CSV export of diagnostic snapshots for reproducible comparisons.
2. Establish baseline test scenarios before implementing automatic optimizations.
3. Test compatibility and stability against a documented mod matrix.
