package org.proto68.jails.commands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.proto68.jails.Jails;
import org.proto68.jails.utils.MessageUtil;

import java.util.UUID;

public class UnjailCommand implements CommandExecutor {

    private final Jails plugin;

    public UnjailCommand(Jails plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {

        if (!sender.hasPermission("jails.unjail")) {
            sender.sendMessage(MessageUtil.get(plugin, "no_permission"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(MessageUtil.get(plugin, "usage_unjail"));
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

        // --- Already unjailed check ---
        if (!plugin.getDatabaseManager().isJailed(uuid)) {
            sender.sendMessage(MessageUtil.get(plugin, "not_jailed"));
            return true;
        }

        // --- Parse reason and silent flag ---
        boolean silent = false;
        String reason = null;

        if (args.length > 1) {
            StringBuilder reasonBuilder = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                if (args[i].equalsIgnoreCase("-s")) {
                    silent = true;
                } else {
                    reasonBuilder.append(args[i]).append(" ");
                }
            }
            String built = reasonBuilder.toString().trim();
            if (!built.isEmpty()) reason = built;
        }

        // --- Resolve staff info ---
        UUID staffUUID = sender instanceof Player p ? p.getUniqueId() : null;
        String staffName = sender.getName();

        // --- Handle online target ---
        Player onlineTarget = target.isOnline() ? target.getPlayer() : null;
        if (onlineTarget != null) {
            Location spawn = plugin.getJailScheduler().getSpawnLocation();
            onlineTarget.teleport(spawn);
            notifyUnjailedPlayer(onlineTarget, staffName, reason);
        }

        // --- Save to DB ---
        plugin.getDatabaseManager().unjailPlayer(uuid, staffUUID, staffName, reason);

        // --- Send webhook message ---
        plugin.getDiscordWebhook().sendUnjail(username, staffName, reason);

        // --- Messaging ---
        String displayReason = reason != null ? reason : "No reason provided";

        if (silent) {
            sender.sendMessage(MessageUtil.get(plugin, "sender_silent_unjail", "player", username, "reason", displayReason));
        } else {
            Bukkit.broadcastMessage(MessageUtil.get(plugin, "broadcast_unjail", "player", username, "staff", staffName, "reason", displayReason));
        }

        return true;
    }

    private void notifyUnjailedPlayer(Player player, String staffName, String reason) {
        String displayReason = reason != null ? reason : "No reason provided";
        player.sendMessage(MessageUtil.get(plugin, "notify_unjailed_title"));
        player.sendMessage(MessageUtil.get(plugin, "notify_unjailed_by", "staff", staffName));
        player.sendMessage(MessageUtil.get(plugin, "notify_unjailed_reason", "reason", displayReason));
    }
}