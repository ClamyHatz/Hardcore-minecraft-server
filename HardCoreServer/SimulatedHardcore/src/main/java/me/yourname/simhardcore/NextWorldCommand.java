package me.yourname.simhardcore;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class NextWorldCommand implements CommandExecutor {

    private final SimulatedHardcore plugin;

    public NextWorldCommand(SimulatedHardcore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        plugin.getResetManager().startReset();

        sender.sendMessage("§cStarting next world...");

        return true;
    }
}