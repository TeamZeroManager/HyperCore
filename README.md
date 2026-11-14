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

See [docs/DIAGNOSTICS.md](docs/DIAGNOSTICS.md) for metric definitions and limitations.

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
- [ ] Add a user-facing diagnostics screen or HUD.
- [ ] Add frame-time statistics using a carefully validated sampling mechanism.
- [ ] Add optional export of diagnostic snapshots for reproducible comparisons.
- [ ] Introduce opt-in optimizations, each independently configurable.
- [ ] Test compatibility and stability against a documented mod matrix.

HyperCore will not claim FPS improvements without repeatable measurements.
