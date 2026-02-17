package net.clearlag.cobblemon.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.clearlag.cobblemon.config.ClearLagConfig;
import net.clearlag.cobblemon.events.ClearLagManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Enregistre toutes les commandes /clearlag.
 *
 * Commandes disponibles (permission level 2 = op) :
 *   /clearlag                    → affiche le statut
 *   /clearlag now                → force un clear immédiat
 *   /clearlag reload             → recharge la config depuis le fichier
 *   /clearlag start              → démarre le scheduler
 *   /clearlag stop               → stoppe le scheduler
 *   /clearlag status             → infos détaillées
 *   /clearlag set interval <n>   → change l'intervalle (secondes)
 *   /clearlag set warn1 <n>      → change le 1er avertissement
 *   /clearlag set warn2 <n>      → change le 2ème avertissement
 *   /clearlag set items <bool>   → active/désactive le clear des items
 *   /clearlag set pokemon <bool> → active/désactive le clear des Pokémon sauvages
 *   /clearlag set xp <bool>      → active/désactive le clear des XP
 *   /clearlag set arrows <bool>  → active/désactive le clear des flèches
 */
public class ClearLagCommand {

    private static final String PREFIX = "§8[§bClearLag§8] §r";
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {

        dispatcher.register(
            CommandManager.literal("clearlag")
                .requires(src -> src.hasPermissionLevel(2))

                // /clearlag (statut rapide)
                .executes(ClearLagCommand::cmdStatus)

                // /clearlag now
                .then(CommandManager.literal("now")
                    .executes(ClearLagCommand::cmdNow))

                // /clearlag reload
                .then(CommandManager.literal("reload")
                    .executes(ClearLagCommand::cmdReload))

                // /clearlag start
                .then(CommandManager.literal("start")
                    .executes(ClearLagCommand::cmdStart))

                // /clearlag stop
                .then(CommandManager.literal("stop")
                    .executes(ClearLagCommand::cmdStop))

                // /clearlag status
                .then(CommandManager.literal("status")
                    .executes(ClearLagCommand::cmdStatusDetailed))

                // /clearlag set ...
                .then(CommandManager.literal("set")

                    .then(CommandManager.literal("interval")
                        .then(CommandManager.argument("seconds", IntegerArgumentType.integer(30, 7200))
                            .executes(ctx -> cmdSetInterval(ctx,
                                IntegerArgumentType.getInteger(ctx, "seconds")))))

                    .then(CommandManager.literal("warn1")
                        .then(CommandManager.argument("seconds", IntegerArgumentType.integer(5, 300))
                            .executes(ctx -> cmdSetWarn1(ctx,
                                IntegerArgumentType.getInteger(ctx, "seconds")))))

                    .then(CommandManager.literal("warn2")
                        .then(CommandManager.argument("seconds", IntegerArgumentType.integer(3, 120))
                            .executes(ctx -> cmdSetWarn2(ctx,
                                IntegerArgumentType.getInteger(ctx, "seconds")))))

                    .then(CommandManager.literal("items")
                        .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                            .executes(ctx -> cmdSetBool(ctx, "items",
                                BoolArgumentType.getBool(ctx, "enabled")))))

                    .then(CommandManager.literal("pokemon")
                        .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                            .executes(ctx -> cmdSetBool(ctx, "pokemon",
                                BoolArgumentType.getBool(ctx, "enabled")))))

                    .then(CommandManager.literal("xp")
                        .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                            .executes(ctx -> cmdSetBool(ctx, "xp",
                                BoolArgumentType.getBool(ctx, "enabled")))))

                    .then(CommandManager.literal("arrows")
                        .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                            .executes(ctx -> cmdSetBool(ctx, "arrows",
                                BoolArgumentType.getBool(ctx, "enabled")))))

                    .then(CommandManager.literal("boats")
                        .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                            .executes(ctx -> cmdSetBool(ctx, "boats",
                                BoolArgumentType.getBool(ctx, "enabled")))))

                    .then(CommandManager.literal("projectiles")
                        .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                            .executes(ctx -> cmdSetBool(ctx, "projectiles",
                                BoolArgumentType.getBool(ctx, "enabled")))))

                    .then(CommandManager.literal("message")
                        .then(CommandManager.literal("warn1")
                            .then(CommandManager.argument("text", StringArgumentType.greedyString())
                                .executes(ctx -> cmdSetMessage(ctx, "warn1",
                                    StringArgumentType.getString(ctx, "text")))))
                        .then(CommandManager.literal("warn2")
                            .then(CommandManager.argument("text", StringArgumentType.greedyString())
                                .executes(ctx -> cmdSetMessage(ctx, "warn2",
                                    StringArgumentType.getString(ctx, "text")))))
                        .then(CommandManager.literal("clear")
                            .then(CommandManager.argument("text", StringArgumentType.greedyString())
                                .executes(ctx -> cmdSetMessage(ctx, "clear",
                                    StringArgumentType.getString(ctx, "text"))))))
                )
        );
    }

    // ─── Handlers ────────────────────────────────────────────────

    private static int cmdStatus(CommandContext<ServerCommandSource> ctx) {
        ClearLagManager mgr = ClearLagManager.get();
        if (mgr == null) { reply(ctx, "§cLe manager n'est pas initialisé."); return 0; }

        long eta = mgr.getSecondsUntilNextClear();
        String status = mgr.isRunning()
                ? "§aACTIF §7— prochain clear dans §e" + eta + "s"
                : "§cARRÊTÉ";
        reply(ctx, "Statut: " + status);
        return 1;
    }

    private static int cmdStatusDetailed(CommandContext<ServerCommandSource> ctx) {
        ClearLagManager mgr = ClearLagManager.get();
        if (mgr == null) { reply(ctx, "§cLe manager n'est pas initialisé."); return 0; }

        ClearLagConfig cfg = ClearLagConfig.get();
        long eta = mgr.getSecondsUntilNextClear();
        long lastMs = mgr.getLastClearTime();
        String lastStr = lastMs > 0
                ? TIME_FMT.format(Instant.ofEpochMilli(lastMs))
                : "Jamais";

        String[] lines = {
            "§b══ ClearLag Status ══",
            "§7Statut:      " + (mgr.isRunning() ? "§aACTIF" : "§cARRÊTÉ"),
            "§7Prochain:    §e" + (eta >= 0 ? eta + "s" : "—"),
            "§7Intervalle:  §f" + cfg.intervalSeconds + "s",
            "§7Avert. 1:    §f-" + cfg.firstWarningSeconds + "s",
            "§7Avert. 2:    §f-" + cfg.secondWarningSeconds + "s",
            "§7Clears tot.: §f" + mgr.getTotalClears(),
            "§7Entités sup: §f" + mgr.getTotalEntitiesRemoved(),
            "§7Dernier:     §f" + lastStr,
            "§7Items:   " + bool(cfg.clearItems)   + "  §7Pokémon: " + bool(cfg.clearWildPokemon),
            "§7XP:      " + bool(cfg.clearXpOrbs)  + "  §7Flèches: " + bool(cfg.clearArrows),
            "§7Bateaux: " + bool(cfg.clearBoats)   + "  §7Projec.: " + bool(cfg.clearProjectiles),
        };

        for (String line : lines) {
            ctx.getSource().sendFeedback(() -> Text.literal(PREFIX + line), false);
        }
        return 1;
    }

    private static int cmdNow(CommandContext<ServerCommandSource> ctx) {
        ClearLagManager mgr = ClearLagManager.get();
        if (mgr == null) { reply(ctx, "§cLe manager n'est pas initialisé."); return 0; }
        reply(ctx, "§aClear forcé en cours...");
        mgr.executeClear();
        return 1;
    }

    private static int cmdReload(CommandContext<ServerCommandSource> ctx) {
        ClearLagManager mgr = ClearLagManager.get();
        if (mgr == null) { reply(ctx, "§cLe manager n'est pas initialisé."); return 0; }
        mgr.reload();
        reply(ctx, "§aConfig rechargée et scheduler redémarré.");
        return 1;
    }

    private static int cmdStart(CommandContext<ServerCommandSource> ctx) {
        ClearLagManager mgr = ClearLagManager.get();
        if (mgr == null) { reply(ctx, "§cLe manager n'est pas initialisé."); return 0; }
        if (mgr.isRunning()) { reply(ctx, "§eScheduler déjà actif."); return 0; }
        mgr.start();
        reply(ctx, "§aScheduler démarré.");
        return 1;
    }

    private static int cmdStop(CommandContext<ServerCommandSource> ctx) {
        ClearLagManager mgr = ClearLagManager.get();
        if (mgr == null) { reply(ctx, "§cLe manager n'est pas initialisé."); return 0; }
        if (!mgr.isRunning()) { reply(ctx, "§eScheduler déjà arrêté."); return 0; }
        mgr.stop();
        reply(ctx, "§cScheduler arrêté.");
        return 1;
    }

    private static int cmdSetInterval(CommandContext<ServerCommandSource> ctx, int seconds) {
        ClearLagConfig cfg = ClearLagConfig.get();
        cfg.intervalSeconds = seconds;
        ClearLagConfig.save();
        ClearLagManager mgr = ClearLagManager.get();
        if (mgr != null) mgr.reload();
        reply(ctx, "§aIntervalle mis à jour: §e" + seconds + "s§a. Scheduler redémarré.");
        return 1;
    }

    private static int cmdSetWarn1(CommandContext<ServerCommandSource> ctx, int seconds) {
        ClearLagConfig cfg = ClearLagConfig.get();
        if (seconds >= cfg.intervalSeconds) {
            reply(ctx, "§cErreur: warn1 doit être inférieur à l'intervalle (" + cfg.intervalSeconds + "s).");
            return 0;
        }
        cfg.firstWarningSeconds = seconds;
        ClearLagConfig.save();
        reply(ctx, "§a1er avertissement: §e-" + seconds + "s");
        return 1;
    }

    private static int cmdSetWarn2(CommandContext<ServerCommandSource> ctx, int seconds) {
        ClearLagConfig cfg = ClearLagConfig.get();
        if (seconds >= cfg.firstWarningSeconds) {
            reply(ctx, "§cErreur: warn2 doit être inférieur à warn1 (" + cfg.firstWarningSeconds + "s).");
            return 0;
        }
        cfg.secondWarningSeconds = seconds;
        ClearLagConfig.save();
        reply(ctx, "§a2ème avertissement: §e-" + seconds + "s");
        return 1;
    }

    private static int cmdSetBool(CommandContext<ServerCommandSource> ctx, String key, boolean value) {
        ClearLagConfig cfg = ClearLagConfig.get();
        switch (key) {
            case "items"      -> cfg.clearItems = value;
            case "pokemon"    -> cfg.clearWildPokemon = value;
            case "xp"         -> cfg.clearXpOrbs = value;
            case "arrows"     -> cfg.clearArrows = value;
            case "boats"      -> cfg.clearBoats = value;
            case "projectiles"-> cfg.clearProjectiles = value;
        }
        ClearLagConfig.save();
        reply(ctx, "§a" + key + " → " + (value ? "§aactivé" : "§cdésactivé") + "§a. Sauvegardé.");
        return 1;
    }

    private static int cmdSetMessage(CommandContext<ServerCommandSource> ctx, String which, String text) {
        ClearLagConfig cfg = ClearLagConfig.get();
        switch (which) {
            case "warn1" -> cfg.firstWarningMessage  = text;
            case "warn2" -> cfg.secondWarningMessage = text;
            case "clear" -> cfg.clearMessage         = text;
        }
        ClearLagConfig.save();
        reply(ctx, "§aMessage '" + which + "' mis à jour.");
        return 1;
    }

    // ─── Utilitaires ─────────────────────────────────────────────

    private static void reply(CommandContext<ServerCommandSource> ctx, String msg) {
        ctx.getSource().sendFeedback(() -> Text.literal(PREFIX + msg), false);
    }

    private static String bool(boolean b) {
        return b ? "§a✔" : "§c✘";
    }
}
