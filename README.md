# HyperCore

**HyperCore** is a Fabric performance diagnostics and optimization project for Minecraft **1.20.1**.

The project is being built around three principles:

1. **Measure before optimizing** — every optimization must have a measurable purpose.
2. **Prefer safe changes** — avoid altering gameplay or world data unless explicitly required.
3. **Keep compatibility visible** — detect the runtime environment and document known interactions with other mods.

## Current target

- Minecraft: 1.20.1
- Mod loader: Fabric
- Java: 17
- Project status: diagnostics foundation in development

## Current diagnostics

The client diagnostics module currently:

- Samples Minecraft-reported FPS every 20 client ticks.
- Records JVM heap usage, committed heap, and maximum heap.
- Records the processor count visible to the JVM and Java/OS version details.
- Keeps a rolling in-memory history of the latest 60 samples.
- Provides an optional diagnostics HUD, toggled with **F8**, showing FPS, JVM heap usage, logical processor count, Java version, OS details, and rolling frame-time statistics (latest, average, and 95th percentile).
- Automatically creates a text report for each world session in `<game directory>/hypercore/sessions/`.
- Writes a sample approximately once per second and flushes it immediately, so previously recorded data is retained even if Minecraft crashes (the closing summary may be absent).
- Includes the detected mod IDs and versions in each report to help investigate modpack compatibility.

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
- [x] Add a lightweight client diagnostics sampler and bounded sample history.
- [ ] Verify the project with a clean Gradle build and in-game launch.
- [x] Add a lightweight, toggleable diagnostics HUD.
- [x] Add rolling frame-time statistics from HUD render intervals, with a 120-sample window.
- [x] Add automatic per-world text reports with periodic durable samples.
- [ ] Establish baseline test scenarios before implementing automatic optimizations.
- [ ] Introduce opt-in optimizations, each independently configurable.
- [ ] Test compatibility and stability against a documented mod matrix.

HyperCore will not claim FPS improvements without repeatable measurements.
