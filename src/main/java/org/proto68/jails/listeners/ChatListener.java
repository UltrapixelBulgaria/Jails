package org.proto68.jails.listeners;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import org.proto68.jails.Jails;

public class ChatListener implements Listener {

    private final Jails plugin;

    public ChatListener(Jails plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();

        // Not jailed → ignore
        if (!plugin.getDatabaseManager().isJailed(sender.getUniqueId())) return;

        event.setCancelled(true);

        // Switch to main thread
        Bukkit.getScheduler().runTask(plugin, () -> {

            String message = "§c[JAIL] §7" + sender.getName() + ": §f" + event.getMessage();

            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionQuery query = container.createQuery();

            String jailRegion = plugin.getConfig().getString("jail.region");

            if (jailRegion == null) return;

            var senderLoc = BukkitAdapter.adapt(sender.getLocation());
            ApplicableRegionSet senderRegions = query.getApplicableRegions(senderLoc);

            boolean senderInJail = senderRegions.getRegions().stream()
                    .anyMatch(r -> r.getId().equalsIgnoreCase(jailRegion));

            if (!senderInJail) return;

            for (Player target : Bukkit.getOnlinePlayers()) {

                var targetLoc = BukkitAdapter.adapt(target.getLocation());
                ApplicableRegionSet targetRegions = query.getApplicableRegions(targetLoc);

                boolean targetInJail = targetRegions.getRegions().stream()
                        .anyMatch(r -> r.getId().equalsIgnoreCase(jailRegion));

                if (targetInJail) {
                    target.sendMessage(message);
                }
            }
        });
    }
}