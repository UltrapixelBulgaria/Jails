package org.proto68.jails.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.proto68.jails.database.JailRecord;
import org.proto68.jails.Jails;
import org.proto68.jails.utils.MessageUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class InfoCommand implements CommandExecutor {

    private final Jails plugin;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public InfoCommand(Jails plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {

        if (!sender.hasPermission("jails.info")) {
            sender.sendMessage(MessageUtil.get(plugin, "no_permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(MessageUtil.get(plugin, "usage_info"));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(MessageUtil.get(plugin, "player_not_found"));
            return true;
        }

        UUID uuid = target.getUniqueId();
        String username = target.getName() != null ? target.getName() : args[1];

        // Fetch async — history can be a large query
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<JailRecord> history = plugin.getDatabaseManager().getJailHistory(uuid);

            // Send back on main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (history.isEmpty()) {
                    sender.sendMessage(MessageUtil.colorize("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
                    sender.sendMessage(MessageUtil.colorize("&6&l Jail History &7— &f" + username));
                    sender.sendMessage(MessageUtil.colorize("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
                    sender.sendMessage(MessageUtil.colorize("  &7No jail records found."));
                    sender.sendMessage(MessageUtil.colorize("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
                    return;
                }

                long totalTime = history.stream().mapToLong(r -> r.time).sum();
                long activeCount = history.stream().filter(JailRecord::isJailed).count();

                sender.sendMessage(MessageUtil.colorize("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
                sender.sendMessage(MessageUtil.colorize("&6&l Jail History &7— &f" + username));
                sender.sendMessage(MessageUtil.colorize("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
                sender.sendMessage(MessageUtil.colorize(
                        "  &7Total Records: &f" + history.size() +
                                "  &7Active: &f" + activeCount +
                                "  &7Total Time Served: &f" + formatDuration(totalTime)
                ));
                sender.sendMessage(MessageUtil.colorize("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));

                for (int i = 0; i < history.size(); i++) {
                    JailRecord record = history.get(i);
                    boolean active = record.isJailed();

                    String statusLabel  = active ? "&a&l● ACTIVE" : "&7&l○ EXPIRED";

                    sender.sendMessage(MessageUtil.colorize(
                            "  &8#" + (i + 1) + " " + statusLabel +
                                    " &8| &7Cell: &f" + record.cell
                    ));
                    sender.sendMessage(MessageUtil.colorize(
                            "  &7Reason: &f" + (record.reason != null ? record.reason : "No reason provided")
                    ));
                    sender.sendMessage(MessageUtil.colorize(
                            "  &7Jailed by: &f" + record.jailedByName +
                                    " &8| &7Duration: &f" + formatDuration(record.time)
                    ));

                    if (active) {
                        sender.sendMessage(MessageUtil.colorize(
                                "  &7Time left: &c" + formatTimeLeft(record.until)
                        ));
                    } else {
                        // Show unjail info if available
                        if (record.unjailedByName != null) {
                            sender.sendMessage(MessageUtil.colorize(
                                    "  &7Unjailed by: &f" + record.unjailedByName +
                                            (record.unjailedByReason != null ? " &8| &7Reason: &f" + record.unjailedByReason : "")
                            ));
                            if (record.unjailedByDate != null) {
                                sender.sendMessage(MessageUtil.colorize(
                                        "  &7Released on: &f" + DATE_FORMAT.format(new Date(record.unjailedByDate.getTime()))
                                ));
                            }
                        } else {
                            sender.sendMessage(MessageUtil.colorize(
                                    "  &7Released: &fSentence expired"
                            ));
                        }
                    }

                    if (record.silent) {
                        sender.sendMessage(MessageUtil.colorize("  &8[Silent jail]"));
                    }

                    if (i < history.size() - 1) {
                        sender.sendMessage(MessageUtil.colorize("  &8&m- - - - - - - - - - - - - - -"));
                    }
                }

                sender.sendMessage(MessageUtil.colorize("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
            });
        });

        return true;
    }

    private String formatDuration(long seconds) {
        if (seconds <= 0) return "0s";
        long d = seconds / 86400; seconds %= 86400;
        long h = seconds / 3600;  seconds %= 3600;
        long m = seconds / 60;    seconds %= 60;
        StringBuilder sb = new StringBuilder();
        if (d > 0) sb.append(d).append("d ");
        if (h > 0) sb.append(h).append("h ");
        if (m > 0) sb.append(m).append("m ");
        if (seconds > 0 || sb.isEmpty()) sb.append(seconds).append("s");
        return sb.toString().trim();
    }

    private String formatTimeLeft(long until) {
        long ms = until - System.currentTimeMillis();
        if (ms <= 0) return "Expired";
        return formatDuration(ms / 1000);
    }
}