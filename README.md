# 🧹 Cobblemon ClearLag

Mod Fabric pour Minecraft 1.21.1 — Clear Lag automatique avec **protection des Pokémon des joueurs**.

---

## 📦 Installation

### Prérequis
- Java 21
- Minecraft **1.21.1**
- [Fabric Loader](https://fabricmc.net/) ≥ 0.16.0
- [Fabric API](https://modrinth.com/mod/fabric-api) pour 1.21.1
- [Cobblemon](https://cobblemon.com/) ≥ 1.6.0

### Compilation
```bash
git clone <ce-repo>
cd cobblemon-clearlag
./gradlew build
# Le .jar est dans build/libs/cobblemon-clearlag-1.0.0.jar
```

### Placement
Copier le `.jar` dans le dossier `mods/` de votre serveur.

---

## ⚙️ Configuration

Le fichier de config est généré automatiquement au premier lancement :
```
config/cobblemon_clearlag.json
```

### Exemple de config complète
```json
{
  "intervalSeconds": 300,
  "firstWarningSeconds": 30,
  "secondWarningSeconds": 10,
  "clearItems": true,
  "clearWildPokemon": true,
  "clearXpOrbs": false,
  "clearArrows": false,
  "clearBoats": false,
  "clearProjectiles": false,
  "clearOverworld": true,
  "clearNether": true,
  "clearEnd": true,
  "firstWarningMessage": "§e⚠ Clear lag dans §c{TIME}§e secondes !",
  "secondWarningMessage": "§c🔴 Clear lag dans §l{TIME}§r§c secondes ! Ramassez vos items !",
  "clearMessage": "§a✅ Clear lag effectué ! Les entités ont été supprimées.",
  "prefix": "§8[§bClearLag§8] §r",
  "logToConsole": true,
  "extraEntityTypes": []
}
```

### Paramètres
| Clé | Type | Défaut | Description |
|-----|------|--------|-------------|
| `intervalSeconds` | int | 300 | Intervalle entre chaque clear (s) |
| `firstWarningSeconds` | int | 30 | 1er avertissement avant le clear (s) |
| `secondWarningSeconds` | int | 10 | 2ème avertissement avant le clear (s) |
| `clearItems` | bool | true | Items au sol |
| `clearWildPokemon` | bool | true | Pokémon sauvages (non-owned) |
| `clearXpOrbs` | bool | false | Orbes d'expérience |
| `clearArrows` | bool | false | Flèches |
| `clearBoats` | bool | false | Bateaux & chariots |
| `clearProjectiles` | bool | false | Boules de feu, snowballs, œufs |
| `clearOverworld` | bool | true | Appliquer en Overworld |
| `clearNether` | bool | true | Appliquer dans le Nether |
| `clearEnd` | bool | true | Appliquer dans l'End |
| `extraEntityTypes` | list | [] | IDs d'entités custom à supprimer |

**Dans les messages :** `{TIME}` est remplacé par les secondes restantes.

---

## 💬 Commandes

Toutes les commandes nécessitent le niveau de permission **2 (op)**.

| Commande | Description |
|----------|-------------|
| `/clearlag` | Affiche le statut rapide |
| `/clearlag status` | Infos détaillées (stats, config active) |
| `/clearlag now` | Force un clear immédiat |
| `/clearlag reload` | Recharge la config et redémarre le scheduler |
| `/clearlag start` | Démarre le scheduler |
| `/clearlag stop` | Met en pause le scheduler |
| `/clearlag set interval <s>` | Change l'intervalle (30–7200s) |
| `/clearlag set warn1 <s>` | Change le 1er avertissement |
| `/clearlag set warn2 <s>` | Change le 2ème avertissement |
| `/clearlag set items <true/false>` | Active/désactive les items |
| `/clearlag set pokemon <true/false>` | Active/désactive les Pokémon sauvages |
| `/clearlag set xp <true/false>` | Active/désactive les XP |
| `/clearlag set arrows <true/false>` | Active/désactive les flèches |
| `/clearlag set boats <true/false>` | Active/désactive les bateaux |
| `/clearlag set projectiles <true/false>` | Active/désactive les projectiles |
| `/clearlag set message warn1 <texte>` | Change le message du 1er warning |
| `/clearlag set message warn2 <texte>` | Change le message du 2ème warning |
| `/clearlag set message clear <texte>` | Change le message de confirmation |

> Les commandes `set` sauvegardent automatiquement dans le fichier JSON.

---

## 🛡️ Protection des Pokémon

Le mod utilise `pokemon.getOwnerUUID() == null` pour détecter les Pokémon sauvages.  
**Les Pokémon capturés / appartenant à un joueur ne sont JAMAIS supprimés**, même s'ils sont en liberté dans le monde.

---

## 📊 Entités supprimées par défaut

- ✅ Items au sol (`minecraft:item`)
- ✅ Pokémon sauvages (`cobblemon:pokemon` sans propriétaire)
- ❌ XP (désactivé, peut causer de la frustration)
- ❌ Flèches, bateaux, projectiles (désactivés par défaut)

---

## 🔧 Entités custom

Ajoutez des IDs dans `extraEntityTypes` pour supprimer des entités de mods tiers :
```json
"extraEntityTypes": ["alexsmobs:fly", "iceandfire:dragon"]
```

---

## 📜 License

MIT — Libre d'utilisation et de modification.
