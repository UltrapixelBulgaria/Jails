package org.proto68.jails.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.proto68.jails.Jails;
import org.proto68.jails.database.DatabaseManager;
import org.proto68.jails.database.JailRecord;

import java.util.UUID;

public class JoinListener implements Listener {

    private final Jails plugin;

    public JoinListener(Jails plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        DatabaseManager databaseManager = plugin.getDatabaseManager();
        JailRecord record = databaseManager.getJailRecord(uuid);
        if (record == null)
            return;

        int active = record.active;
        boolean in_jail = record.inJail;
        String reason = record.reason;
        int cell = record.cell;


        if (active != 1 && in_jail){
            plugin.getJailScheduler().releasePlayer(uuid);
            databaseManager.updateInJail(uuid, false, active);
        } else if (active == 1 && !in_jail) {
            plugin.getJailScheduler().jailPlayer(uuid, cell, reason);
            databaseManager.updateInJail(uuid, true, active);
        }


    }
}
