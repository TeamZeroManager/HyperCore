package com.teamzeromanager.hypercore.client;

import com.teamzeromanager.hypercore.HyperCore;
import com.teamzeromanager.hypercore.client.diagnostics.FrameTimeMonitor;
import com.teamzeromanager.hypercore.client.diagnostics.PerformanceHud;
import com.teamzeromanager.hypercore.client.diagnostics.PerformanceMonitor;
import com.teamzeromanager.hypercore.client.diagnostics.SessionLogger;
import net.fabricmc.api.ClientModInitializer;

/**
 * Client entrypoint for diagnostics and client-only optimization systems.
 */
public final class HyperCoreClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FrameTimeMonitor.register();
        PerformanceMonitor.register();
        PerformanceHud.register();
        SessionLogger.register();
        HyperCore.LOGGER.info("HyperCore client diagnostics initialized.");
    }
}
