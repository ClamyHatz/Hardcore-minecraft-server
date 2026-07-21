package me.yourname.simhardcore;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public class SimulatedHardcore extends JavaPlugin implements Listener {

    private ResetManager resetManager;
    private boolean resetInProgress = false;
    private boolean paused = false;

    private int totalDeaths;
    private int totalResets;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        paused = getConfig().getBoolean("paused");
        totalDeaths = getConfig().getInt("stats.deaths", 0);
        totalResets = getConfig().getInt("stats.resets", 0);
        getLogger().info("Hardcore paused: " + paused);

        getServer().getPluginManager().registerEvents(this, this);

        resetManager = new ResetManager(this);

        getCommand("nextworld").setExecutor(new NextWorldCommand(this));
        getCommand("pause").setExecutor(new PauseCommand(this));

        // Register commands
        this.getCommand("resets").setExecutor((sender, command, label, args) -> {
            sender.sendMessage("§aWorld resets: " + totalResets);
            sender.sendMessage("§aTotal deaths: " + totalDeaths);
            return true;
        });

        this.getCommand("resetcounters").setExecutor((sender, command, label, args) -> {
            totalResets = 0;
            totalDeaths = 0;

            getConfig().set("stats.deaths", 0);
            getConfig().set("stats.resets", 0);
            saveConfig();

            sender.sendMessage("§cCounters reset.");
            return true;
        });

        getLogger().info("SimulatedHardcore enabled.");
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {

        if (resetInProgress) return;

        totalDeaths++;

        getConfig().set("stats.deaths", totalDeaths);
        saveConfig();

        Player player = event.getEntity();

        if (isPaused()) {
            Bukkit.broadcastMessage("§eA player died, but hardcore mode is paused.");
            return;
        }

        Bukkit.getScheduler().runTaskLater(this, () -> {
            player.setGameMode(GameMode.SPECTATOR);
        }, 1L);

        resetInProgress = true;

        Bukkit.broadcastMessage("§cA player has died. Hardcore world will reset!");

        resetManager.startReset();

        int countdown = getConfig().getInt("countdown-seconds", 15);

        Bukkit.getScheduler().runTaskLater(this, () -> {
            resetInProgress = false;
        }, 20L * countdown);
    }

    public void incrementWorldReset() {
        totalResets++;

        getConfig().set("stats.resets", totalResets);
        saveConfig();
    }

    @Override
    public void onDisable() {
        getConfig().set("stats.deaths", totalDeaths);
        getConfig().set("stats.resets", totalResets);
        saveConfig();
    }

    // Optional getters
    public int getWorldResets() { return totalResets; }
    public int getTotalDeaths() { return totalDeaths; }
    public boolean isPaused() { return paused; }
    public void setPaused(boolean paused) {
        this.paused = paused;

        getConfig().set("paused", paused);
        saveConfig();
    }
    public ResetManager getResetManager() { return resetManager; }
}
