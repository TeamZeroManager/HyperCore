package com.teamzeromanager.hypercore;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common entrypoint for HyperCore.
 *
 * Keep client-only Minecraft classes out of this initializer so the mod
 * remains safe to load in dedicated server environments.
 */
public final class HyperCore implements ModInitializer {
    public static final String MOD_ID = "hypercore";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("HyperCore {} initialized.", getClass().getPackage().getImplementationVersion() == null
                ? "development"
                : getClass().getPackage().getImplementationVersion());
    }
}
