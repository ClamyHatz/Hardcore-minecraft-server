package me.yourname.simhardcore;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class PauseCommand implements CommandExecutor {

    private final SimulatedHardcore plugin;

    public PauseCommand(SimulatedHardcore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        boolean newState = !plugin.isPaused();

        plugin.setPaused(newState);

        if (newState) {
            sender.sendMessage("§eHardcore mode paused.");
        } else {
            sender.sendMessage("§aHardcore mode resumed.");
        }

        return true;
    }
}