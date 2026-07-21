package me.yourname.simhardcore;

import java.util.Iterator;

import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Statistic;
import org.mvplugins.multiverse.core.MultiverseCore;
import org.mvplugins.multiverse.core.world.WorldManager;
import org.mvplugins.multiverse.core.world.MultiverseWorld;

import java.io.File;
import java.util.Random;
import java.util.UUID;

public class ResetManager {

    private final JavaPlugin plugin;

    private static final String HARDCORE_WORLD = "hardcore";
    private static final String NETHER_HARDCORE_WORLD = "hardcore_nether";
    private static final String END_HARDCORE_WORLD = "hardcore_the_end";
    private static final String WAITING_WORLD = "waiting";

    public ResetManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /* =========================
       PUBLIC ENTRY POINT
       ========================= */

    public void startReset() {
        plugin.getLogger().info("Hardcore death detected. Starting reset sequence...");

        int countdown = 15; // hardcoded 15 seconds now

        new BukkitRunnable() {
            int timeLeft = countdown;

            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendTitle(
                            "§cWorld resetting in...",
                            "§e" + timeLeft + " seconds",
                            0, 20, 0
                    );
                }

                if (timeLeft <= 0) {
                    cancel();
                    teleportAllToWaiting();
                    resetHardcoreWorld();
                }

                timeLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L); // runs every second
    }

        /* =========================
        TELEPORT PLAYERS
        ========================= */

    private void teleportAllToWaiting() {
        World waiting = Bukkit.getWorld(WAITING_WORLD);

        if (waiting == null) {
            plugin.getLogger().severe("Waiting world does not exist!");
            return;
        }

        Location spawn = waiting.getSpawnLocation();

        resetAllPlayers();

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.teleport(spawn);
            player.setGameMode(GameMode.ADVENTURE);
            player.sendMessage("§6Waiting for a new world to be created...");
        }
    }

    private void startTeleportCountdown(World hardcore) {

        int seconds = 5;

        new BukkitRunnable() {
            int time = seconds;

            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendTitle(
                            "§cNew World Incoming",
                            "§eTeleporting in " + time + "…",
                            0, 20, 0
                    );
                }

                if (time <= 0) {
                    cancel();
                    teleportPlayersToHardcore(hardcore);
                    return;
                }

                time--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    /* =========================
    RESET WORLD LOGIC
    ========================= */

    private void resetHardcoreWorld() {
        new BukkitRunnable() {
            @Override
            public void run() {

                unloadAndDeleteWorld(HARDCORE_WORLD);
                unloadAndDeleteWorld(NETHER_HARDCORE_WORLD);
                unloadAndDeleteWorld(END_HARDCORE_WORLD);

                ((SimulatedHardcore) plugin).incrementWorldReset();

                createNewHardcoreWorlds();
            }
        }.runTaskLater(plugin, 40L); // safety delay
    }

    private void unloadAndDeleteWorld(String name) {
        World world = Bukkit.getWorld(name);
        if (world != null) {
            Bukkit.unloadWorld(world, false);
        }
        deleteWorldFolder(name);
    }

    public void createNewHardcoreWorlds() {
        long seed = new Random().nextLong();

        // Overworld
        WorldCreator overworldCreator = new WorldCreator(HARDCORE_WORLD)
                .environment(World.Environment.NORMAL)
                .seed(seed)
                .type(WorldType.NORMAL);

        // Nether
        WorldCreator netherCreator = new WorldCreator(NETHER_HARDCORE_WORLD)
                .environment(World.Environment.NETHER)
                .seed(seed)
                .type(WorldType.NORMAL);

        // End
        WorldCreator endCreator = new WorldCreator(END_HARDCORE_WORLD)
                .environment(World.Environment.THE_END)
                .seed(seed)
                .type(WorldType.NORMAL);

        // Create worlds
        World overworld = Bukkit.createWorld(overworldCreator);
        World nether = Bukkit.createWorld(netherCreator);
        World theEnd = Bukkit.createWorld(endCreator);

        if (overworld == null || nether == null || theEnd == null) {
            plugin.getLogger().severe("Failed to create one or more hardcore worlds!");
            return;
        }

        // Safe spawn in overworld
        Location spawn = overworld.getSpawnLocation();
        spawn.setY(overworld.getHighestBlockYAt(spawn) + 1);
        overworld.setSpawnLocation(spawn);
        overworld.setGameRule(GameRule.KEEP_INVENTORY, false);

        plugin.getLogger().info("New hardcore worlds generated with seed: " + seed);

        // Start countdown/teleport logic
        startTeleportCountdown(overworld);
    }


    /* =========================
       FINAL TELEPORT
       ========================= */

    private void teleportPlayersToHardcore(World hardcore) {
        Location spawn = hardcore.getSpawnLocation();

        resetAllPlayers();

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.teleport(spawn);
            Location safe = new Location(
                    hardcore,
                    hardcore.getSpawnLocation().getBlockX(),
                    hardcore.getHighestBlockYAt(hardcore.getSpawnLocation()) + 1,
                    hardcore.getSpawnLocation().getBlockZ()
            );
            hardcore.setSpawnLocation(safe);
            player.setGameMode(GameMode.SURVIVAL);
            player.sendMessage("§cA new hardcore world has begun!");
        }

        int countup = 3;

        new BukkitRunnable() {
            int timeRight = countup;
            int resets = ((SimulatedHardcore) plugin).getWorldResets();

            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendTitle(
                            "§cAttempt",
                            "§e #" + resets,
                            0, 20, 0
                    );
                }

                if (timeRight <= 0) {
                    cancel();
                }

                timeRight--;
            }
        }.runTaskTimer(plugin, 0L, 20L); // runs every second
    }

    /* =========================
       UTIL
       ========================= */

    public void deleteWorldFolder(String worldName) {
        File worldFolder = new File(worldName);

        if (!worldFolder.exists()) return;

        try {
            // Unload world safely first
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                Bukkit.unloadWorld(world, false);
            }

            // Delete all files/folders recursively
            deleteFolderRecursively(worldFolder);
            plugin.getLogger().info("Deleted world folder: " + worldName);

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to delete world folder " + worldName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void deleteFolderRecursively(File folder) {
        if (folder.isDirectory()) {
            for (File file : folder.listFiles()) {
                deleteFolderRecursively(file);
            }
        }
        folder.delete();
    }

    public void resetAllPlayers() {
        File playerFolder = new File(plugin.getDataFolder().getParentFile(), "Multiverse-Inventories/players");

        if (!playerFolder.exists()) return;

        for (File file : playerFolder.listFiles()) {
            if (file.isFile() && file.getName().endsWith(".yml")) {
                // This resets inventories for everyone in the hardcore group
                file.delete(); // removes old inventory data
            }
        }

        // Optional: force online players to reload their data
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.getInventory().clear();
            player.getEnderChest().clear();

            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.setSaturation(20f);
            player.setExhaustion(0f);

            player.setFireTicks(0);
            player.setFallDistance(0);

            player.setTotalExperience(0);
            player.setLevel(0);
            player.setExp(0f);

            // Remove potion effects
            player.getActivePotionEffects().forEach(effect ->
                player.removePotionEffect(effect.getType())
            );

            // Reset advancements
            Iterator<Advancement> it = Bukkit.advancementIterator();
            while (it.hasNext()) {
                Advancement adv = it.next();
                AdvancementProgress progress = player.getAdvancementProgress(adv);

                for (String criteria : progress.getAwardedCriteria()) {
                    progress.revokeCriteria(criteria);
                }
            }

            // Reset statistics
            for (Statistic stat : Statistic.values()) {
                try {
                    player.setStatistic(stat, 0);
                } catch (Exception ignored) {}
            }
        }
    }

}