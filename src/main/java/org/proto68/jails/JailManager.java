package org.proto68.jails;

import org.bukkit.event.HandlerList;
import org.proto68.jails.database.DatabaseManager;
import org.proto68.jails.listeners.*;

import java.sql.SQLException;
import java.util.logging.Level;

public class JailManager {

    private final Jails plugin;
    private DatabaseManager databaseManager;
    private JailScheduler jailScheduler;
    private DiscordWebhook discordWebhook;

    public JailManager(Jails plugin) {
        this.plugin = plugin;
    }

    public void enable() throws SQLException {
        jailScheduler = new JailScheduler(plugin);
        discordWebhook = new DiscordWebhook(plugin);
        registerEvents();
        connectDatabase();
        registerPlaceholders();
    }

    public void disable() {
        disconnectDatabase();
    }

    public void reload(){
        plugin.reloadConfig();
        HandlerList.unregisterAll(plugin);
        disconnectDatabase();

        if (plugin.getConfig().getBoolean("enable")) {
            registerEvents();
            connectDatabase();
        }
    }

    // --- Getters ---

    public DiscordWebhook getDiscordWebhook() {
        return discordWebhook;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public JailScheduler getJailScheduler() {
        return jailScheduler;
    }

    // --- Private helpers ---

    private void registerPlaceholders() {
        if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new JailPlaceholderExpansion(plugin).register();
            plugin.getLogger().info("PlaceholderAPI hooked successfully.");
        } else {
            plugin.getLogger().warning("PlaceholderAPI not found — placeholders will not work.");
        }
    }

    private void registerEvents() {
        plugin.getServer().getPluginManager().registerEvents(new JoinListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new ChatListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new CommandListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new ExitJailListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new CMIListener(plugin), plugin);
    }

    private void connectDatabase(){
        databaseManager = new DatabaseManager(plugin.getConfig(), plugin.getLogger(), plugin);
        try {
            databaseManager.connect();
            databaseManager.loadAndScheduleJails();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void disconnectDatabase() {
        if (databaseManager == null) return;
        try {
            databaseManager.disconnect();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to disconnect database: " + e.getMessage());
            plugin.getLogger().log(Level.SEVERE, "Error while disconnecting from the DB", e);
        }
    }
}