package net.clearlag.cobblemon.events;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.clearlag.cobblemon.config.ClearLagConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Gère le scheduler de clear lag et l'exécution du clear.
 */
public class ClearLagManager {

    private static ClearLagManager instance;
    private final MinecraftServer server;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ClearLag-Scheduler");
        t.setDaemon(true);
        return t;
    });

    private ScheduledFuture<?> mainTask;
    private boolean running = false;

    // Stats
    private int totalClears = 0;
    private int totalEntitiesRemoved = 0;
    private long lastClearTime = 0;

    private ClearLagManager(MinecraftServer server) {
        this.server = server;
    }

    public static ClearLagManager create(MinecraftServer server) {
        instance = new ClearLagManager(server);
        return instance;
    }

    public static ClearLagManager get() {
        return instance;
    }

    /** Démarre le scheduler de clear lag */
    public void start() {
        if (running) return;
        running = true;
        scheduleNext();
        log("Clear Lag démarré. Intervalle: " + ClearLagConfig.get().intervalSeconds + "s");
    }

    /** Arrête le scheduler */
    public void stop() {
        running = false;
        if (mainTask != null) mainTask.cancel(false);
        log("Clear Lag arrêté.");
    }

    /** Recharge la config et redémarre */
    public void reload() {
        stop();
        ClearLagConfig.load();
        start();
        log("Config rechargée.");
    }

    private void scheduleNext() {
        if (!running) return;
        ClearLagConfig cfg = ClearLagConfig.get();
        int interval = cfg.intervalSeconds;
        int warn1 = cfg.firstWarningSeconds;
        int warn2 = cfg.secondWarningSeconds;

        // Avertissement 1
        if (warn1 < interval) {
            scheduler.schedule(() -> broadcastWarning(warn1),
                    interval - warn1, TimeUnit.SECONDS);
        }

        // Avertissement 2
        if (warn2 < interval && warn2 < warn1) {
            scheduler.schedule(() -> broadcastWarning(warn2),
                    interval - warn2, TimeUnit.SECONDS);
        }

        // Clear principal
        mainTask = scheduler.schedule(() -> {
            executeClear();
            scheduleNext(); // re-planifie
        }, interval, TimeUnit.SECONDS);
    }

    /** Diffuse un message d'avertissement */
    private void broadcastWarning(int timeLeft) {
        ClearLagConfig cfg = ClearLagConfig.get();
        String msg = timeLeft == cfg.firstWarningSeconds
                ? cfg.firstWarningMessage
                : cfg.secondWarningMessage;

        String formatted = cfg.format(msg, timeLeft);
        server.execute(() ->
            server.getPlayerManager().broadcast(Text.literal(formatted), false)
        );
    }

    /**
     * Effectue le clear des entités dans toutes les dimensions configurées.
     * Appelé dans le thread du scheduler, puis dispatché sur le thread principal du serveur.
     */
    public void executeClear() {
        server.execute(() -> {
            ClearLagConfig cfg = ClearLagConfig.get();
            int removed = 0;

            List<ServerWorld> worlds = getTargetWorlds();

            for (ServerWorld world : worlds) {
                removed += clearWorld(world, cfg);
            }

            // Message final
            String msg = cfg.formatFinal(cfg.clearMessage);
            server.getPlayerManager().broadcast(Text.literal(msg), false);

            totalClears++;
            totalEntitiesRemoved += removed;
            lastClearTime = System.currentTimeMillis();

            if (cfg.logToConsole) {
                log("Clear #" + totalClears + " — " + removed + " entité(s) supprimée(s).");
            }
        });
    }

    /**
     * Supprime les entités configurées dans un monde donné.
     * @return nombre d'entités supprimées
     */
    private int clearWorld(ServerWorld world, ClearLagConfig cfg) {
        List<Entity> toKill = new ArrayList<>();

        for (Entity entity : world.iterateEntities()) {
            if (shouldKill(entity, cfg)) {
                toKill.add(entity);
            }
        }

        for (Entity entity : toKill) {
            entity.discard();
        }

        return toKill.size();
    }

    /**
     * Détermine si une entité doit être supprimée selon la config.
     */
    private boolean shouldKill(Entity entity, ClearLagConfig cfg) {
        // Items au sol
        if (cfg.clearItems && entity instanceof ItemEntity) return true;

        // Pokémon sauvages (non-owned)
        if (cfg.clearWildPokemon && entity instanceof PokemonEntity pokemon) {
            // On ne supprime que les Pokémon sans propriétaire
            return pokemon.getOwnerUUID() == null;
        }

        // Orbes XP
        if (cfg.clearXpOrbs && entity instanceof ExperienceOrbEntity) return true;

        // Flèches
        if (cfg.clearArrows && entity instanceof ArrowEntity) return true;

        // Bateaux et chariots
        if (cfg.clearBoats && (entity instanceof BoatEntity || entity instanceof AbstractMinecartEntity)) return true;

        // Projectiles
        if (cfg.clearProjectiles && (
                entity instanceof FireballEntity ||
                entity instanceof SmallFireballEntity ||
                entity instanceof SnowballEntity ||
                entity instanceof EggEntity)) return true;

        // Types custom
        if (!cfg.extraEntityTypes.isEmpty()) {
            String entityId = entity.getType().toString();
            for (String extra : cfg.extraEntityTypes) {
                if (entityId.contains(extra)) return true;
            }
        }

        return false;
    }

    /**
     * Retourne la liste des ServerWorld à cleaner selon la config.
     */
    private List<ServerWorld> getTargetWorlds() {
        ClearLagConfig cfg = ClearLagConfig.get();
        List<ServerWorld> worlds = new ArrayList<>();

        if (cfg.clearOverworld) {
            ServerWorld w = server.getWorld(World.OVERWORLD);
            if (w != null) worlds.add(w);
        }
        if (cfg.clearNether) {
            ServerWorld w = server.getWorld(World.NETHER);
            if (w != null) worlds.add(w);
        }
        if (cfg.clearEnd) {
            ServerWorld w = server.getWorld(World.END);
            if (w != null) worlds.add(w);
        }

        return worlds;
    }

    // ── Getters pour les commandes ────────────────────────────

    public int getTotalClears() { return totalClears; }
    public int getTotalEntitiesRemoved() { return totalEntitiesRemoved; }
    public long getLastClearTime() { return lastClearTime; }
    public boolean isRunning() { return running; }

    /** Retourne le temps restant avant le prochain clear (en secondes, approximatif) */
    public long getSecondsUntilNextClear() {
        if (mainTask == null || mainTask.isCancelled()) return -1;
        return mainTask.getDelay(TimeUnit.SECONDS);
    }

    private void log(String msg) {
        System.out.println("[ClearLag] " + msg);
    }

    public void shutdown() {
        stop();
        scheduler.shutdownNow();
        instance = null;
    }
}
