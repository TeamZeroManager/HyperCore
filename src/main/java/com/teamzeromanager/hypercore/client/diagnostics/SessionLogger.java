package com.teamzeromanager.hypercore.client.diagnostics;

import com.teamzeromanager.hypercore.client.optimization.GpuSaver;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.MinecraftClient;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Locale;

/**
 * Writes one durable text report for each continuous Minecraft world session.
 *
 * Samples are appended and flushed once per second. If the game crashes or is
 * forcibly terminated, the report should still contain all previously flushed
 * samples, although its final summary may be missing.
 */
public final class SessionLogger {
    private static final SessionLogger INSTANCE = new SessionLogger();
    private static final int SAMPLE_INTERVAL_TICKS = 20;
    private static final DateTimeFormatter FILE_TIME =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DISPLAY_TIME =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault());

    private BufferedWriter writer;
    private Path activeFile;
    private long sessionStartNanos;
    private Instant sessionStart;
    private int ticksSinceSample;
    private long sampleCount;
    private long fpsTotal;
    private int minFps = Integer.MAX_VALUE;
    private int maxFps;
    private double maxHeapUsedMiB;

    private SessionLogger() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(INSTANCE::onClientTick);
        Runtime.getRuntime().addShutdownHook(new Thread(
                () -> INSTANCE.finishSession("JVM shutdown"),
                "HyperCore-session-shutdown"
        ));
    }

    private synchronized void onClientTick(MinecraftClient client) {
        if (client.world == null) {
            if (writer != null) {
                finishSession("World closed");
            }
            return;
        }

        if (writer == null) {
            startSession(client);
        }

        if (writer == null) {
            return;
        }

        ticksSinceSample++;
        if (ticksSinceSample >= SAMPLE_INTERVAL_TICKS) {
            ticksSinceSample = 0;
            writeSample(client);
        }
    }

    private void startSession(MinecraftClient client) {
        try {
            Path directory = FabricLoader.getInstance().getGameDir()
                    .resolve("hypercore")
                    .resolve("sessions");
            Files.createDirectories(directory);

            sessionStart = Instant.now();
            sessionStartNanos = System.nanoTime();
            activeFile = directory.resolve("session_" + FILE_TIME.format(sessionStart) + ".txt");
            writer = Files.newBufferedWriter(activeFile, StandardCharsets.UTF_8);
            sampleCount = 0;
            fpsTotal = 0;
            minFps = Integer.MAX_VALUE;
            maxFps = 0;
            maxHeapUsedMiB = 0.0;
            ticksSinceSample = 0;

            writeLine("HYPERCORE - INFORME DE SESION");
            writeLine("Inicio: " + DISPLAY_TIME.format(sessionStart));
            writeLine("Modo: " + (client.isInSingleplayer() ? "Singleplayer" : "Multiplayer"));
            writeLine("Dimension inicial: " + client.world.getRegistryKey().getValue());
            writeLine("Minecraft: 1.20.1");
            writeLine("Java: " + System.getProperty("java.version", "unknown"));
            writeLine("Sistema operativo: " + System.getProperty("os.name", "unknown")
                    + " " + System.getProperty("os.version", "unknown"));
            writeLine("Procesadores visibles para Java: " + Runtime.getRuntime().availableProcessors());
            writeLine("Heap maximo JVM (MiB): " + format(toMiB(Runtime.getRuntime().maxMemory())));
            writeLine("");
            writeLine("MODS DETECTADOS (id | version)");
            FabricLoader.getInstance().getAllMods().stream()
                    .sorted(Comparator.comparing(mod -> mod.getMetadata().getId()))
                    .forEach(this::writeMod);
            writeLine("");
            writeLine("MUESTRAS (una por segundo aprox.)");
            writeLine("fecha_hora | tiempo_s | fps | gpu_saver | frame_ms_ultimo | frame_ms_media_ventana"
                    + " | frame_ms_p95_ventana | heap_usado_mib | heap_comprometido_mib | heap_maximo_mib");
            writer.flush();
        } catch (IOException exception) {
            closeWriterQuietly();
            activeFile = null;
            System.err.println("[HyperCore] No se pudo iniciar el registro de sesion: " + exception.getMessage());
        }
    }

    private void writeMod(ModContainer mod) {
        writeLine(mod.getMetadata().getId() + " | " + mod.getMetadata().getVersion().getFriendlyString());
    }

    private void writeSample(MinecraftClient client) {
        Runtime runtime = Runtime.getRuntime();
        long usedBytes = runtime.totalMemory() - runtime.freeMemory();
        double usedMiB = toMiB(usedBytes);
        int fps = client.getCurrentFps();
        String gpuSaverState = GpuSaver.isEnabled() ? "ON" : "OFF";
        FrameTimeMonitor frameMonitor = FrameTimeMonitor.getInstance();
        double latestFrame = frameMonitor.getLatestFrameTimeMs();
        double averageFrame = frameMonitor.getAverageFrameTimeMs();
        double percentile95 = frameMonitor.get95thPercentileMs();
        double elapsedSeconds = (System.nanoTime() - sessionStartNanos) / 1_000_000_000.0;

        sampleCount++;
        fpsTotal += fps;
        minFps = Math.min(minFps, fps);
        maxFps = Math.max(maxFps, fps);
        maxHeapUsedMiB = Math.max(maxHeapUsedMiB, usedMiB);

        writeLine(DISPLAY_TIME.format(Instant.now())
                + " | " + format(elapsedSeconds)
                + " | " + fps
                + " | " + gpuSaverState
                + " | " + format(latestFrame)
                + " | " + format(averageFrame)
                + " | " + format(percentile95)
                + " | " + format(usedMiB)
                + " | " + format(toMiB(runtime.totalMemory()))
                + " | " + format(toMiB(runtime.maxMemory())));

        // Flush each sample so a crash loses at most the currently written line.
        try {
            if (writer != null) {
                writer.flush();
            }
        } catch (IOException exception) {
            System.err.println("[HyperCore] Error al guardar una muestra: " + exception.getMessage());
            closeWriterQuietly();
        }
    }

    private synchronized void finishSession(String reason) {
        if (writer == null) {
            return;
        }

        long durationNanos = Math.max(0L, System.nanoTime() - sessionStartNanos);
        writeLine("");
        writeLine("RESUMEN");
        writeLine("Fin: " + DISPLAY_TIME.format(Instant.now()));
        writeLine("Motivo de cierre: " + reason);
        writeLine("Duracion (segundos): " + format(durationNanos / 1_000_000_000.0));
        writeLine("Numero de muestras: " + sampleCount);
        if (sampleCount > 0) {
            writeLine("FPS promedio de las muestras: " + format((double) fpsTotal / sampleCount));
            writeLine("FPS minimo observado: " + minFps);
            writeLine("FPS maximo observado: " + maxFps);
            writeLine("Heap usado maximo observado (MiB): " + format(maxHeapUsedMiB));
        } else {
            writeLine("No se alcanzaron a registrar muestras de rendimiento.");
        }

        Path completedFile = activeFile;
        closeWriterQuietly();
        activeFile = null;
        System.out.println("[HyperCore] Informe de sesion guardado en: " + completedFile);
    }

    private void writeLine(String line) {
        if (writer == null) {
            return;
        }
        try {
            writer.write(line);
            writer.newLine();
        } catch (IOException exception) {
            System.err.println("[HyperCore] Error escribiendo el informe: " + exception.getMessage());
            closeWriterQuietly();
        }
    }

    private void closeWriterQuietly() {
        if (writer != null) {
            try {
                writer.flush();
                writer.close();
            } catch (IOException ignored) {
                // Preserve the original failure; the file remains best-effort.
            }
        }
        writer = null;
    }

    private static double toMiB(long bytes) {
        return bytes / (1024.0 * 1024.0);
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}