package net.clearlag.cobblemon.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration du mod CobblemonClearLag.
 * Le fichier est sauvegardé en JSON dans config/cobblemon_clearlag.json
 */
public class ClearLagConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("cobblemon_clearlag.json");

    private static ClearLagConfig instance;

    // ── Timing ──────────────────────────────────────────────────
    /** Intervalle entre chaque clear (en secondes) */
    public int intervalSeconds = 300;

    /** Délai du 1er avertissement avant le clear (en secondes) */
    public int firstWarningSeconds = 30;

    /** Délai du 2ème avertissement avant le clear (en secondes) */
    public int secondWarningSeconds = 10;

    // ── Entités ──────────────────────────────────────────────────
    /** Supprimer les items au sol */
    public boolean clearItems = true;

    /** Supprimer les Pokémon sauvages (non-owned) */
    public boolean clearWildPokemon = true;

    /** Supprimer les orbes d'expérience */
    public boolean clearXpOrbs = false;

    /** Supprimer les flèches */
    public boolean clearArrows = false;

    /** Supprimer les bateaux et chariots abandonnés */
    public boolean clearBoats = false;

    /** Supprimer les projectiles (boules de feu, snowballs, oeufs...) */
    public boolean clearProjectiles = false;

    // ── Dimensions ──────────────────────────────────────────────
    /** Appliquer dans l'Overworld */
    public boolean clearOverworld = true;

    /** Appliquer dans le Nether */
    public boolean clearNether = true;

    /** Appliquer dans l'End */
    public boolean clearEnd = true;

    // ── Messages ────────────────────────────────────────────────
    /** Message envoyé au 1er avertissement. {TIME} = secondes restantes */
    public String firstWarningMessage = "§e⚠ Clear lag dans §c{TIME}§e secondes !";

    /** Message envoyé au 2ème avertissement. {TIME} = secondes restantes */
    public String secondWarningMessage = "§c🔴 Clear lag dans §l{TIME}§r§c secondes ! Ramassez vos items !";

    /** Message envoyé après le clear */
    public String clearMessage = "§a✅ Clear lag effectué ! Les entités ont été supprimées.";

    /** Préfixe affiché devant tous les messages */
    public String prefix = "§8[§bClearLag§8] §r";

    // ── Avancé ──────────────────────────────────────────────────
    /** Activer les logs dans la console du serveur */
    public boolean logToConsole = true;

    /** Types d'entités custom supplémentaires à supprimer (IDs Minecraft) */
    public List<String> extraEntityTypes = new ArrayList<>();

    // ────────────────────────────────────────────────────────────

    public static ClearLagConfig get() {
        if (instance == null) load();
        return instance;
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                instance = GSON.fromJson(reader, ClearLagConfig.class);
                if (instance == null) instance = new ClearLagConfig();
            } catch (IOException e) {
                System.err.println("[ClearLag] Erreur de lecture config: " + e.getMessage());
                instance = new ClearLagConfig();
            }
        } else {
            instance = new ClearLagConfig();
            save();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(instance, writer);
            }
        } catch (IOException e) {
            System.err.println("[ClearLag] Erreur de sauvegarde config: " + e.getMessage());
        }
    }

    /** Formate un message en remplaçant {TIME} par la valeur donnée */
    public String format(String msg, int timeLeft) {
        return prefix + msg.replace("{TIME}", String.valueOf(timeLeft));
    }

    public String formatFinal(String msg) {
        return prefix + msg;
    }
}
