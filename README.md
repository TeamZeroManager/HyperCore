# HyperCore

**HyperCore** is a Fabric performance diagnostics and optimization project for Minecraft **1.20.1**.

The project follows three principles:

1. **Measure before optimizing** — every optimization must have a measurable purpose.
2. **Prefer safe changes** — avoid altering gameplay or world data unless explicitly required.
3. **Keep compatibility visible** — detect the runtime environment and document known interactions with other mods.

## Current target

- Minecraft: 1.20.1
- Mod loader: Fabric
- Java: 17
- Project status: diagnostics plus an opt-in client rendering preset.

## Current diagnostics

- Samples Minecraft-reported FPS every 20 client ticks.
- Records JVM heap usage, committed heap, and maximum heap.
- Records processor count visible to the JVM and Java/OS details.
- Keeps a rolling in-memory history of the latest 60 samples.
- Provides an optional diagnostics HUD, toggled with **F8**; its text and layout are cached on client ticks rather than rebuilt every rendered frame.
- Measures rolling HUD render intervals (latest, average, p95); these are not direct GPU timings.
- Creates a text report for each world session in `<game directory>/hypercore/sessions/`, sampling approximately once per second.
- Includes detected mod IDs and versions in each report.
- Records whether the HyperCore GPU saver is enabled on every performance sample, making before/after test segments easier to identify.

## GPU saver preset

Press **F7** to toggle the optional, reversible GPU-oriented preset:

- Caps Minecraft at **50 FPS maximum** (or keeps a lower existing cap).
- Reduces render distance to **8 chunks maximum**.
- Reduces entity render distance to **75% maximum**.
- Sets graphics to Fast, clouds off, particles to Minimal, ambient occlusion off, and entity shadows off.
- Sets biome blend radius to 0.
- Saves the previous values to a recovery file before changing settings.\n- Restores only settings that still match the preset, preserving manual changes made while it is active.\n- Recovers the previous settings on the next launch if Minecraft closed or crashed while the preset was active.

The preset uses Minecraft 1.20.1's public GameOptions API. It reduces client rendering workload but does not read GPU temperatures, change GPU voltage/clock speeds, or guarantee a specific temperature reduction. It cannot directly override shader-pack settings or every renderer-specific option; test with the exact Sodium/Iris configuration.

See [docs/DIAGNOSTICS.md](docs/DIAGNOSTICS.md) for metric definitions, report format, and limitations.

## Development

Open the project as a Gradle project in IntelliJ IDEA or another Gradle-compatible IDE.

Build the mod:

```bash
gradle build
```

The remapped mod JAR will be generated in `build/libs/`.

## Roadmap

- [x] Establish the initial project structure for Fabric 1.20.1 / Java 17.
- [x] Add lightweight client diagnostics and a bounded sample history.
- [x] Add a toggleable diagnostics HUD and rolling frame-time statistics.
- [x] Add automatic per-world text reports.
- [x] Add an opt-in, reversible GPU saver preset for core vanilla graphics options.
- [x] Verify a clean Gradle build in GitHub Actions for the GPU saver branch.\n- [ ] Verify crash recovery and manual-change preservation in an in-game test.
- [ ] Establish repeatable baseline scenarios and compare results with/without each optimization.
- [ ] Investigate renderer-specific optimization hooks for Sodium/Iris before adding invasive mixins.

HyperCore will not claim FPS or temperature improvements without repeatable measurements.
