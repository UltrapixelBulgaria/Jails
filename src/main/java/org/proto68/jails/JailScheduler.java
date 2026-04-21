package org.proto68.jails;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.proto68.jails.database.JailRecord;
import org.proto68.jails.utils.MessageUtil;

import java.util.UUID;

public class JailScheduler {

    private final Jails plugin;

    public JailScheduler(Jails plugin) {
        this.plugin = plugin;
    }

    public void scheduleRelease(UUID uuid, long until) {
        long delayTicks = (until - System.currentTimeMillis()) / 50;

        if (delayTicks <= 0) {
            releasePlayer(uuid, true);
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (plugin.getDatabaseManager().isJailed(uuid))
                    releasePlayer(uuid, true);
            }
        }.runTaskLater(plugin, delayTicks);
    }

    public void releasePlayer(UUID uuid, boolean sendDiscordNotification) {
        String systemReason = plugin.getConfig().getString("messages.release_system_reason", "Sentence expired");
        plugin.getDatabaseManager().unjailPlayer(uuid, null, "SYSTEM", systemReason);

        // Fetch username for webhook before player object check
        JailRecord record = plugin.getDatabaseManager().getJailRecord(uuid);
        String username = record != null ? record.username : uuid.toString();

        if (sendDiscordNotification)
            plugin.getDiscordWebhook().sendAutoRelease(username);

        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;

        Location spawn = getSpawnLocation();

        if (spawn != null) {
            player.teleport(spawn);
        } else {
            // Fallback to world spawn if config is missing/broken
            player.teleport(player.getWorld().getSpawnLocation());
            plugin.getLogger().warning("Jail spawn not configured — fell back to world spawn. Use /jailsetspawn to fix this.");
        }

        player.sendMessage(MessageUtil.get(plugin, "released"));
    }

    public void jailPlayer(UUID uuid, int cell, String reason) {
        Location loc = getCellLocation(cell);

        if (loc == null) {
            plugin.getLogger().warning("Could not jail " + uuid + " — cell " + cell + " location is invalid.");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(uuid);
        if (!target.isOnline()) return;

        Player player = target.getPlayer();
        if (player == null) return;

        player.teleport(loc);
        player.sendMessage(MessageUtil.get(plugin, "notify_jailed_title"));
        player.sendMessage(MessageUtil.get(plugin, "notify_jailed_reason", "reason", reason != null ? reason : "No reason provided"));
    }

    // --- Helpers ---

    public Location getSpawnLocation() {
        String path = "jail.spawn";
        if (!plugin.getConfig().contains(path)) return null;

        World world = Bukkit.getWorld(plugin.getConfig().getString(path + ".world", ""));
        if (world == null) return null;

        return new Location(
                world,
                plugin.getConfig().getDouble(path + ".x"),
                plugin.getConfig().getDouble(path + ".y"),
                plugin.getConfig().getDouble(path + ".z"),
                (float) plugin.getConfig().getDouble(path + ".yaw"),
                (float) plugin.getConfig().getDouble(path + ".pitch")
        );
    }

    private Location getCellLocation(int cell) {
        String path = "jail.cells." + cell;
        if (!plugin.getConfig().contains(path)) return null;

        World world = Bukkit.getWorld(plugin.getConfig().getString(path + ".world", ""));
        if (world == null) return null;

        return new Location(
                world,
                plugin.getConfig().getDouble(path + ".x"),
                plugin.getConfig().getDouble(path + ".y"),
                plugin.getConfig().getDouble(path + ".z"),
                (float) plugin.getConfig().getDouble(path + ".yaw"),
                (float) plugin.getConfig().getDouble(path + ".pitch")
        );
    }
}