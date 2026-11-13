package com.teamzeromanager.hypercore.client;

import com.teamzeromanager.hypercore.HyperCore;
import net.fabricmc.api.ClientModInitializer;

/**
 * Client entrypoint. Client diagnostics and client-only optimizations
 * will be registered here in later development stages.
 */
public final class HyperCoreClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HyperCore.LOGGER.info("HyperCore client environment detected.");
    }
}
