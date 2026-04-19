package org.proto68.jails.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NonNull;
import org.proto68.jails.Jails;

import java.sql.SQLException;

public class ReloadCommand implements CommandExecutor {
    private final Jails plugin;

    public ReloadCommand(Jails plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {
        // Checking the sender for required permission
        if (sender.hasPermission("jails.reload")) {
            // Getting the time before the reload
            long before = System.currentTimeMillis();
            // Reloading the plugin
            try {
                plugin.reload();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            // Getting the time after the reload
            long after = System.currentTimeMillis();
            // Sending message with the total time for the reload
            sender.sendMessage("Successfully reloaded UPBGJail in " + (after - before) + "ms!");
        }
        return true;
    }
}