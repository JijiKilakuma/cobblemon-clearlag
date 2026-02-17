package net.clearlag.cobblemon;

import net.clearlag.cobblemon.commands.ClearLagCommand;
import net.clearlag.cobblemon.config.ClearLagConfig;
import net.clearlag.cobblemon.events.ClearLagManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Point d'entrée principal du mod Cobblemon ClearLag.
 */
public class CobblemonClearLag implements ModInitializer {

    public static final String MOD_ID = "cobblemon_clearlag";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Cobblemon ClearLag - Chargement...");

        // Charger la config
        ClearLagConfig.load();
        LOGGER.info("Config chargée depuis config/cobblemon_clearlag.json");

        // Enregistrer les commandes
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ClearLagCommand.register(dispatcher);
            LOGGER.info("Commandes /clearlag enregistrées.");
        });

        // Démarrer le manager quand le serveur est prêt
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ClearLagManager manager = ClearLagManager.create(server);
            manager.start();
            LOGGER.info("ClearLag Manager démarré. Intervalle: {}s", ClearLagConfig.get().intervalSeconds);
        });

        // Arrêter proprement à la fermeture du serveur
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            ClearLagManager mgr = ClearLagManager.get();
            if (mgr != null) {
                mgr.shutdown();
                LOGGER.info("ClearLag Manager arrêté proprement.");
            }
        });

        LOGGER.info("Cobblemon ClearLag - Initialisé !");
    }
}
