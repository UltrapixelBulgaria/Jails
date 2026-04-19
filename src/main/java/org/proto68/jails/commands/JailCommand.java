package org.proto68.jails.commands;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.proto68.jails.Jails;
import org.proto68.jails.utils.MessageUtil;
import org.proto68.jails.utils.TimeParser;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class JailCommand implements CommandExecutor {

    private final Jails plugin;

    public JailCommand(Jails plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String @NonNull [] args) {

        if (!sender.hasPermission("jails.jail")) {
            sender.sendMessage(MessageUtil.get(plugin, "no_permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(MessageUtil.get(plugin, "usage_jail"));
            return true;
        }

        // --- Resolve target ---
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(MessageUtil.get(plugin, "player_not_found"));
            return true;
        }

        UUID uuid = target.getUniqueId();
        String username = target.getName() != null ? target.getName() : args[0];

        // --- Already jailed check ---
        if (plugin.getDatabaseManager().isJailed(uuid)) {
            sender.sendMessage(MessageUtil.get(plugin, "already_jailed"));
            return true;
        }

        // --- Parse time ---
        long seconds;
        try {
            seconds = TimeParser.parseTime(args[1]);
        } catch (Exception e) {
            sender.sendMessage(MessageUtil.get(plugin, "invalid_time"));
            return true;
        }

        // --- Parse reason and silent flag ---
        boolean silent = false;
        String reason = null;

        if (args.length > 2) {
            StringBuilder reasonBuilder = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                if (args[i].equalsIgnoreCase("-s")) {
                    silent = true;
                } else {
                    reasonBuilder.append(args[i]).append(" ");
                }
            }
            String built = reasonBuilder.toString().trim();
            if (!built.isEmpty()) reason = built;
        }

        // --- Resolve cell ---
        int cell = getNextAvailableCell();
        if (cell == -1) {
            sender.sendMessage(MessageUtil.get(plugin, "no_cells"));
            return true;
        }

        // --- Load cell location ---
        Location loc = getCellLocation(cell);
        if (loc == null) {
            sender.sendMessage(MessageUtil.get(plugin, "invalid_cell"));
            return true;
        }

        // --- Resolve staff info ---
        UUID staffUUID = sender instanceof Player p ? p.getUniqueId() : null;
        String staffName = sender.getName();

        // --- Handle online target ---
        String ip = null;
        Player onlineTarget = target.isOnline() ? target.getPlayer() : null;

        if (onlineTarget != null) {
            if (onlineTarget.getAddress() != null) {
                ip = onlineTarget.getAddress().getAddress().getHostAddress();
            }
            onlineTarget.teleport(loc);
            notifyJailedPlayer(onlineTarget, staffName, reason, args[1]);
        }

        // --- Save to database ---
        plugin.getDatabaseManager().jailPlayerAsync(
                uuid, username, ip,
                staffUUID, staffName,
                reason, cell, seconds, silent
        );

        // --- Send webhook message ---
        plugin.getDiscordWebhook().sendJail(username, staffName, String.valueOf(cell), args[1], reason);

        // --- Schedule release ---
        long until = System.currentTimeMillis() + (seconds * 1000L);
        plugin.getJailScheduler().scheduleRelease(uuid, until);

        // --- Messaging ---
        String displayReason = reason != null ? reason : "No reason provided";

        if (silent) {
            sender.sendMessage(MessageUtil.get(plugin, "sender_silent",
                    "player", username, "cell", String.valueOf(cell), "time", args[1], "reason", displayReason));
        } else {
            Bukkit.broadcastMessage(MessageUtil.get(plugin, "broadcast", "player",
                    username, "staff", staffName, "time", args[1], "reason", displayReason));
        }

        return true;
    }

    // --- Helpers ---

    private void notifyJailedPlayer(Player player, String staffName, String reason, String time) {
        String displayReason = reason != null ? reason : "No reason provided";
        player.sendMessage(MessageUtil.get(plugin, "notify_jailed_title"));
        player.sendMessage(MessageUtil.get(plugin, "notify_jailed_by", "staff", staffName));
        player.sendMessage(MessageUtil.get(plugin, "notify_jailed_reason", "reason", displayReason));
        player.sendMessage(MessageUtil.get(plugin, "notify_jailed_duration", "time", time));
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

    public int getNextAvailableCell() {
        Set<Integer> occupied = plugin.getDatabaseManager().getActiveCells();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("jail.cells");

        if (section == null) return -1;

        List<Integer> allCells = section.getKeys(false)
                .stream()
                .map(Integer::parseInt)
                .sorted()
                .toList();

        for (int cell : allCells) {
            if (!occupied.contains(cell)) return cell;
        }

        // All cells occupied — pick random
        return allCells.isEmpty() ? -1 : allCells.get(new Random().nextInt(allCells.size()));
    }
}