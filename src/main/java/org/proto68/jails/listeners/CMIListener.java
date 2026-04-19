package org.proto68.jails.listeners;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.proto68.jails.database.JailRecord;
import org.proto68.jails.Jails;
import org.proto68.jails.utils.MessageUtil;

import java.util.UUID;

public class CMIListener implements Listener {

    private final Jails plugin;

    public CMIListener(Jails plugin) {
        this.plugin = plugin;
    }

    // Intercept player running /whois
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerWhois(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage().toLowerCase();

        if (!message.startsWith("/whois") && !message.startsWith("/cmi whois")) return;

        String[] args = event.getMessage().trim().split("\\s+");
        // /whois <player> or /cmi whois <player>
        String targetName = extractTargetName(args);
        if (targetName == null) return;

        Player sender = event.getPlayer();

        // Delay by 1 tick so CMI's own /whois output appears first
        Bukkit.getScheduler().runTaskLater(plugin, () ->
                appendJailInfo(sender, targetName), 1L);
    }

    // Also intercept console running /whois
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsoleWhois(ServerCommandEvent event) {
        String cmd = event.getCommand().toLowerCase();

        if (!cmd.startsWith("whois") && !cmd.startsWith("cmi whois")) return;

        String[] args = event.getCommand().trim().split("\\s+");
        String targetName = extractTargetName(args);
        if (targetName == null) return;

        Bukkit.getScheduler().runTaskLater(plugin, () ->
                appendJailInfo(event.getSender(), targetName), 1L);
    }

    private void appendJailInfo(org.bukkit.command.CommandSender sender, String targetName) {
        // Resolve UUID
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        if (!target.hasPlayedBefore() && !target.isOnline()) return;

        UUID uuid = target.getUniqueId();
        JailRecord record = plugin.getDatabaseManager().getJailRecord(uuid);

        boolean jailed = record != null && record.isJailed();

        // Append jail section below CMI's whois output
        sender.sendMessage(MessageUtil.colorize("&8&m--------------------"));
        sender.sendMessage(MessageUtil.colorize("&6&lUPBGJail Info"));

        if (jailed) {
            String timeLeft = formatTimeLeft(record.until);
            sender.sendMessage(MessageUtil.colorize("&7Jailed: &c✔ Yes"));
            sender.sendMessage(MessageUtil.colorize("&7Cell: &f" + record.cell));
            sender.sendMessage(MessageUtil.colorize("&7Reason: &f" + (record.reason != null ? record.reason : "No reason")));
            sender.sendMessage(MessageUtil.colorize("&7Jailed by: &f" + record.jailedByName));
            sender.sendMessage(MessageUtil.colorize("&7Time left: &f" + timeLeft));
        } else {
            sender.sendMessage(MessageUtil.colorize("&7Jailed: &a✘ No"));
        }

        sender.sendMessage(MessageUtil.colorize("&8&m--------------------"));
    }

    private String extractTargetName(String[] args) {
        // /whois <player> → args[1]
        // /cmi whois <player> → args[2]
        if (args.length >= 2 && args[0].equalsIgnoreCase("/whois")) return args[1];
        if (args.length >= 3 && args[0].equalsIgnoreCase("/cmi"))   return args[2];
        if (args.length >= 2 && !args[0].startsWith("/"))           return args[1]; // console (no slash)
        return null;
    }

    private String formatTimeLeft(long until) {
        long ms = until - System.currentTimeMillis();
        if (ms <= 0) return "Expired";
        long s = ms / 1000;
        long d = s / 86400; s %= 86400;
        long h = s / 3600;  s %= 3600;
        long m = s / 60;    s %= 60;
        StringBuilder sb = new StringBuilder();
        if (d > 0) sb.append(d).append("d ");
        if (h > 0) sb.append(h).append("h ");
        if (m > 0) sb.append(m).append("m ");
        if (s > 0 || sb.isEmpty()) sb.append(s).append("s");
        return sb.toString().trim();
    }
}