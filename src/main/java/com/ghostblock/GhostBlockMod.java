package com.ghostblock;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GhostBlockMod implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("ghostblock");

    @Override
    public void onInitializeClient() {
        // Drive the state machine every tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> DesyncTracker.tick());

        // Reset all state on disconnect so stale desync data never
        // bleeds into the next session / relog
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            DesyncTracker.reset();
            LOGGER.debug("Disconnected — DesyncTracker reset");
        });

        LOGGER.info("Ghost Block Fix loaded");
    }
}
