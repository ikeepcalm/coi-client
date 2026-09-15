package dev.ua.ikeepcalm.coi;

import net.fabricmc.api.ModInitializer;

/**
 * The common-side entrypoint. COI Client is a client-only mod, so this exists
 * only to satisfy the {@code main} entrypoint Fabric expects — everything the
 * mod actually does starts in {@code CoiClientMod}.
 */
public class CoiClient implements ModInitializer {

    @Override
    public void onInitialize() {
        CoiLog.LOG.info("Initializing COI Client");
    }
}
