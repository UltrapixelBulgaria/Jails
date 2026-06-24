package org.proto68.jails.commands;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
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
            return false;
        }

        // Permission
        if (!player.hasPermission("jails.admin")) {
            player.sendMessage(MessageUtil.get(plugin, "no_permission"));
            return false;
        }

        // Argument check
        if (args.length < 2) {
            player.sendMessage(MessageUtil.get(plugin, "usage_setcell"));
            return false;
        }

        // Region set check
        String regionName = plugin.getConfig().getString("jail.region");
        if (regionName == null){
            player.sendMessage(MessageUtil.get(plugin, "region_not_set"));
            return false;
        }

        RegionManager regionManager = WorldGuard.getInstance()
                .getPlatform()
                .getRegionContainer()
                .get(BukkitAdapter.adapt(player.getWorld()));

        ProtectedRegion region;

        try {
            region = regionManager.getRegion(regionName);
            if (region == null) {
                player.sendMessage(MessageUtil.get(plugin, "no_region", "region", regionName));
                return false;
            }
        } catch (NullPointerException e) {
            player.sendMessage(MessageUtil.get(plugin, "no_region", "region", regionName));
            return false;
        }


        // Cell in region check
        Location loc = player.getLocation();
        BlockVector3 pos = BlockVector3.at(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        if (!region.contains(pos)) {
            player.sendMessage(MessageUtil.get(plugin, "cell_outside_region", "region", regionName));
            return false;
        }

        int cellNumber;

        try {
            cellNumber = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(MessageUtil.get(plugin, "invalid_int"));
            return false;
        }

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