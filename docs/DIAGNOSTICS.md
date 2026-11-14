# HyperCore diagnostics

## Current implementation

The client diagnostics module samples the Minecraft client once every 20 client ticks.

Each in-memory performance sample records:

- Current client FPS reported by Minecraft.
- JVM heap usage (used, committed, and maximum).
- Number of processors visible to the JVM.
- Java version.
- Operating system name and version.
- A monotonic timestamp for ordering samples.

The monitor retains at most 60 samples in memory. Oldest samples are discarded when the history is full.

## Automatic world-session reports

HyperCore creates a separate UTF-8 text report for each continuous world session at:

`<Minecraft game directory>/hypercore/sessions/session_YYYYMMDD_HHMMSS_mmm.txt`

A session starts automatically when the client enters a world and ends when the client leaves it. The logger:

- Writes the session start time, singleplayer/multiplayer mode, initial dimension, Java/OS information, available processor count, and maximum JVM heap.
- Lists detected mod IDs and versions, which helps identify the modpack and investigate compatibility.
- Appends a performance sample approximately once per second: timestamp, elapsed session time, FPS, configured FPS limit, VSync state, render distance, entity render distance, GPU saver ON/OFF state, latest frame interval, rolling frame-time average and p95, and JVM heap used/committed/maximum.
- Writes a closing summary with duration, sample count, average/minimum/maximum sampled FPS, and maximum observed used heap.
- Flushes every sample to disk. If Minecraft crashes or is forcibly terminated, earlier samples should remain available, but the closing summary will be missing.

The file is plain text and can be opened with Notepad. Share the complete report after a test session so the measurements and mod list can be reviewed together.

## Important measurement notes

- Heap usage is JVM heap usage, not total system RAM usage.
- The processor count is the number of processors reported by the JVM; it is not a CPU utilization percentage.
- FPS is the value reported by Minecraft at the sampling moment, not a frame-time benchmark.
- FPS and JVM samples are collected on the client tick event, not on every rendered frame.
- Frame time is measured between Fabric HUD render callbacks and stored as a rolling window of up to 120 intervals. It approximates frame pacing from the client render loop; it is not a GPU timer and does not isolate rendering, driver, or display latency.
- The logged frame-time average and p95 are rolling-window values at each sample, not whole-session percentiles.
- The HUD refreshes the average and 95th-percentile frame interval at most four times per second to keep diagnostic overhead low.
- The logger does not change graphics settings, garbage-collect, or modify world data.
- Session reports do not by themselves prove that HyperCore improved performance. For a valid before/after comparison, repeat the same route or activity, world, graphics settings, render distance, mod list, and approximate warm-up period with and without the optimization being evaluated.

## Current HUD

Press **F8** to show or hide the diagnostics panel. It displays the latest sampled FPS and JVM memory values, basic runtime details, the latest frame interval, the rolling average, and the 95th-percentile frame interval. The rolling frame-time window contains up to 120 intervals.

## Next diagnostics milestones

1. Establish baseline test scenarios before implementing automatic optimizations.
2. Add optional machine-level metrics where reliable cross-platform measurements are available.
3. Test compatibility and stability against a documented mod matrix.
