package net.clearlag.cobblemon;

import net.fabricmc.api.DedicatedServerModInitializer;

/**
 * Entrypoint dédié au serveur.
 * Peut être utilisé pour des initialisations spécifiques serveur si besoin.
 */
public class CobblemonClearLagServer implements DedicatedServerModInitializer {

    @Override
    public void onInitializeServer() {
        CobblemonClearLag.LOGGER.info("Cobblemon ClearLag - Mode serveur dédié activé.");
    }
}
