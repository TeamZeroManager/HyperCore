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
- Provides an optional diagnostics HUD, toggled with **F8**.
- Measures rolling HUD render intervals (latest, average, p95); these are not direct GPU timings.
- Creates a text report for each world session in `<game directory>/hypercore/sessions/`, sampling approximately once per second.
- Includes detected mod IDs and versions in each report.

## GPU saver preset

Press **F7** to toggle the optional, reversible GPU-oriented preset:

- Caps Minecraft at **45 FPS** (or keeps a lower existing cap).
- Reduces render distance to **8 chunks maximum**.
- Reduces entity render distance to **75% maximum**.
- Saves the previous values and restores them when the preset is disabled.

This preset reduces rendering workload; it does not read GPU temperatures, change GPU voltage/clock speeds, or guarantee a specific temperature reduction. It changes Minecraft's standard video options and should be tested with the user's renderer/mod combination (including Sodium/Iris).

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
- [x] Add an opt-in, reversible GPU saver preset.
- [ ] Verify the latest code with a clean Gradle build and in-game launch.
- [ ] Establish repeatable baseline scenarios and compare results with/without each optimization.
- [ ] Expand GPU/render optimizations only when their impact and compatibility can be measured.

HyperCore will not claim FPS or temperature improvements without repeatable measurements.
