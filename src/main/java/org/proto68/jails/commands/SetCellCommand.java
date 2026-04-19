package org.proto68.jails.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.proto68.jails.Jails;
import org.proto68.jails.utils.MessageUtil;

public class SetCellCommand implements CommandExecutor {

    private final Jails plugin;

    public SetCellCommand(Jails plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String @NotNull [] args) {

        // Must be player
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.get(plugin, "player_only"));
            return true;
        }

        // Permission
        if (!player.hasPermission("jails.setcell")) {
            player.sendMessage(MessageUtil.get(plugin, "no_permission"));
            return true;
        }

        // Argument check
        if (args.length < 2) {
            player.sendMessage(MessageUtil.get(plugin, "usage_setcell"));
            return true;
        }

        int cellNumber;

        try {
            cellNumber = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(MessageUtil.get(plugin, "invalid_int"));
            return true;
        }

        Location loc = player.getLocation();

        String path = "jail.cells." + cellNumber;

        // Save location
        plugin.getConfig().set(path + ".world", loc.getWorld().getName());
        plugin.getConfig().set(path + ".x", loc.getX());
        plugin.getConfig().set(path + ".y", loc.getY());
        plugin.getConfig().set(path + ".z", loc.getZ());
        plugin.getConfig().set(path + ".yaw", "-90");
        plugin.getConfig().set(path + ".pitch", "0");

        plugin.saveConfig();

        player.sendMessage(MessageUtil.get(plugin, "cell_set", "cell", String.valueOf(cellNumber)));

        return true;
    }
}