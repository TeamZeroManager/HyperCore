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
- Project status: initial development scaffold

## Development

Open the project as a Gradle project in IntelliJ IDEA or another Gradle-compatible IDE.

Build the mod:

```bash
gradle build
```

The remapped mod JAR will be generated in `build/libs/`.

## Roadmap

- [ ] Establish a reproducible baseline and diagnostics layer.
- [ ] Collect client performance metrics without expensive per-frame work.
- [ ] Add hardware and runtime profiling.
- [ ] Introduce opt-in optimizations, each independently configurable.
- [ ] Test compatibility and stability against a documented mod matrix.

HyperCore will not claim FPS improvements without repeatable measurements.
