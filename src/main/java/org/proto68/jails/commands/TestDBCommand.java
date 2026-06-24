package org.proto68.jails.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.proto68.jails.Jails;
import org.proto68.jails.utils.MessageUtil;

import java.sql.Connection;
import java.sql.SQLException;

public class TestDBCommand implements CommandExecutor {

    private final Jails plugin;

    public TestDBCommand(Jails plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {

        if (!sender.hasPermission("jails.admin")) {
            sender.sendMessage(MessageUtil.get(plugin, "no_permission"));
            return true;
        }

        sender.sendMessage(MessageUtil.colorize("&7Testing database connection..."));

        // Run off the main thread — this performs real network I/O
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            long start = System.currentTimeMillis();
            boolean success;
            String errorMessage = null;

            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                success = conn != null && !conn.isClosed() && conn.isValid(5);
            } catch (SQLException e) {
                success = false;
                errorMessage = e.getMessage();
            }

            long elapsed = System.currentTimeMillis() - start;
            boolean finalSuccess = success;
            String finalError = errorMessage;

            // Hop back to the main thread to message the sender
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (finalSuccess) {
                    sender.sendMessage(MessageUtil.colorize(
                            "&aDatabase connection successful! &7(" + elapsed + "ms)"));
                } else {
                    sender.sendMessage(MessageUtil.colorize(
                            "&cDatabase connection failed: &7" + finalError));
                }
            });
        });

        return true;
    }
}