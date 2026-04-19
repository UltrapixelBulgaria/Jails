package org.proto68.jails.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.proto68.jails.Jails;
import org.proto68.jails.utils.MessageUtil;

public class SetSpawnCommand implements CommandExecutor {

    private final Jails plugin;

    public SetSpawnCommand(Jails plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NonNull [] args) {

        if (!sender.hasPermission("jails.setspawn")) {
            sender.sendMessage(MessageUtil.get(plugin, "no_permission"));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.get(plugin, "player_only"));
            return true;
        }

        plugin.getConfig().set("jail.spawn.world", player.getWorld().getName());
        plugin.getConfig().set("jail.spawn.x",     player.getLocation().getX());
        plugin.getConfig().set("jail.spawn.y",     player.getLocation().getY());
        plugin.getConfig().set("jail.spawn.z",     player.getLocation().getZ());
        plugin.getConfig().set("jail.spawn.yaw",   player.getLocation().getYaw());
        plugin.getConfig().set("jail.spawn.pitch", player.getLocation().getPitch());
        plugin.saveConfig();

        sender.sendMessage(MessageUtil.get(plugin, "spawn_set"));

        return true;
    }
}