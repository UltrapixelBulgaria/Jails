package org.proto68.jails.listeners;

import java.util.Objects;
import java.util.UUID;

import net.raidstone.wgevents.events.RegionLeftEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.proto68.jails.Jails;
import org.proto68.jails.database.DatabaseManager;
import org.proto68.jails.database.JailRecord;

public class ExitJailListener implements Listener {
    private final Jails plugin;

    public ExitJailListener(Jails plugin) {
        this.plugin = plugin;
    }

    @EventHandler(
            priority = EventPriority.LOWEST
    )
    public void onRegionLeft(RegionLeftEvent event) {
        Player player = Bukkit.getPlayer(event.getUUID());

        if (player != null) {
            String regionName = event.getRegionName();
            String jailRegion = Objects.requireNonNull(plugin.getConfig().getString("jail.region", ""));

            if (jailRegion.equals(regionName)) {

                DatabaseManager databaseManager = plugin.getDatabaseManager();
                UUID uuid = player.getUniqueId();
                JailRecord record = databaseManager.getJailRecord(uuid);
                if (record == null)
                    return;

                if (record.active == 1){
                    String reason = record.reason;
                    int cell = record.cell;
                    plugin.getJailScheduler().jailPlayer(uuid, cell, reason);
                }
            }

        }
    }
}