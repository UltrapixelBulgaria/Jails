package org.proto68.jails;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NonNull;
import org.proto68.jails.commands.*;
import org.proto68.jails.completers.JailTabCompleter;
import org.proto68.jails.completers.UpJailTabCompleter;
import org.proto68.jails.database.DatabaseManager;

import java.sql.SQLException;
import java.util.Objects;

public final class Jails extends JavaPlugin {

    private JailManager jailManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        jailManager = new JailManager(this);

        if (!getConfig().getBoolean("enable")) {
            getLogger().warning("UPBGJail is not enabled in the config!");
            return;
        }

        try {
            jailManager.enable();
        } catch (SQLException e) {
            getLogger().severe("Failed to enable UPBGJail: " + e.getMessage());
            throw new RuntimeException(e);
        }

        registerCommands();
        getLogger().info("UPBGJail is enabled!");
    }

    @Override
    public void onDisable() {
        if (jailManager != null) jailManager.disable();
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, Command command, @NonNull String label, String @NonNull [] args) {
        if (!command.getName().equalsIgnoreCase("upjail")) return false;

        if (args.length == 0) {
            sender.sendMessage("Usage: /upjail <subcommand>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload"    -> new ReloadCommand(this).onCommand(sender, command, label, args);
            case "setregion" -> new SetRegionCommand(this).onCommand(sender, command, label, args);
            case "setcell"   -> new SetCellCommand(this).onCommand(sender, command, label, args);
            case "setspawn"  -> new SetSpawnCommand(this).onCommand(sender, command, label, args);
            case "info"      -> new InfoCommand(this).onCommand(sender, command, label, args);
            default          -> sender.sendMessage("Unknown subcommand.");
        }

        return true;
    }

    // --- Delegates — keeps existing call sites working ---

    public DiscordWebhook getDiscordWebhook() {
        return jailManager.getDiscordWebhook();
    }

    public DatabaseManager getDatabaseManager() {
        return jailManager.getDatabaseManager();
    }

    public JailScheduler getJailScheduler() {
        return jailManager.getJailScheduler();
    }

    public void reload() throws SQLException {
        jailManager.reload();
    }

    // --- Private ---

    private void registerCommands() {
        Objects.requireNonNull(getCommand("upjail")).setExecutor(this);
        Objects.requireNonNull(getCommand("upjail")).setTabCompleter(new UpJailTabCompleter());

        JailTabCompleter jailTabCompleter = new JailTabCompleter(this);

        Objects.requireNonNull(getCommand("jail")).setExecutor(new JailCommand(this));
        Objects.requireNonNull(getCommand("jail")).setTabCompleter(jailTabCompleter);

        Objects.requireNonNull(getCommand("unjail")).setExecutor(new UnjailCommand(this));
        Objects.requireNonNull(getCommand("unjail")).setTabCompleter(jailTabCompleter);

    }
}