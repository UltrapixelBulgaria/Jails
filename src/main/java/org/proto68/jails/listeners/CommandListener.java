package org.proto68.jails.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import org.proto68.jails.Jails;

import java.util.List;

public class CommandListener implements Listener {

    private final Jails plugin;
    private final List<String> whitelist;

    public CommandListener(Jails plugin) {
        this.plugin = plugin;
        this.whitelist = plugin
                .getConfig()
                .getStringList("jail.command-whitelist");
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {

        var player = event.getPlayer();

        // Not jailed → allow
        if (!plugin.getDatabaseManager().isJailed(player.getUniqueId())) return;

        String message = event.getMessage().toLowerCase();

        // Extract command without "/"
        String command = message.split(" ")[0].substring(1);

        // Check whitelist
        if (whitelist.contains(command)) {
            return;
        }

        // Cancel command
        event.setCancelled(true);
        player.sendMessage("§cYou cannot use this command while jailed.");
    }
}