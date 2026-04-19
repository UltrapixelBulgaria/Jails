package org.proto68.jails.commands;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.proto68.jails.Jails;
import org.proto68.jails.utils.MessageUtil;

public class SetRegionCommand implements CommandExecutor {

    private final Jails plugin;

    public SetRegionCommand(Jails plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String @NotNull [] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.get(plugin, "player_only"));
            return true;
        }

        if (!player.hasPermission("jails.setregion")) {
            player.sendMessage(MessageUtil.get(plugin, "no_permission"));
            return true;
        }

        // Check argument
        if (args.length < 2) {
            player.sendMessage(MessageUtil.get(plugin, "usage_setregion"));
            return true;
        }

        String regionName = args[1];

        // Get RegionManager
        RegionManager regionManager = WorldGuard.getInstance()
                .getPlatform()
                .getRegionContainer()
                .get(BukkitAdapter.adapt(player.getWorld()));

        if (regionManager == null) {
            player.sendMessage(MessageUtil.get(plugin, "no_world_guard"));
            return true;
        }

        ProtectedRegion region = regionManager.getRegion(regionName);

        if (region == null) {
            player.sendMessage(MessageUtil.get(plugin, "no_region", "region", regionName));
            return true;
        }

        plugin.getConfig().set("jail.region", regionName);
        plugin.saveConfig();

        player.sendMessage(MessageUtil.get(plugin, "region_set", "region", regionName));

        return true;
    }
}