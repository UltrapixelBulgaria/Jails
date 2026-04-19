package org.proto68.jails.completers;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.*;

public class UpJailTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(@NonNull CommandSender sender, @NonNull Command command, @NonNull String alias, String @NonNull [] args) {
        List<String> suggestions = new ArrayList<>();

        Player player = null;
        if (sender instanceof Player) player = (Player) sender;

        if (command.getName().equalsIgnoreCase("upjail")) {
            if (args.length == 1) {
                if (player == null){
                    suggestions.addAll(Arrays.asList("testDB", "reload"));
                } else {
                    if (player.hasPermission("jails.setregion")) {
                        suggestions.add("setRegion");
                    }
                    if (player.hasPermission("jails.setcell")) {
                        suggestions.add("setCell");
                    }
                    if (player.hasPermission("jails.testdb")) {
                        suggestions.add("testDB");
                    }
                    if (player.hasPermission("jails.reload")){
                        suggestions.add("reload");
                    }
                    if (player.hasPermission("jails.setspawn")){
                        suggestions.add("setSpawn");
                    }
                    if (player.hasPermission("jails.info")){
                        suggestions.add("info");
                    }
                }
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("setregion")) {
                    assert player != null;
                    suggestions.addAll(getRegionNames(player));
                }
                if (args[0].equalsIgnoreCase("setcell")){
                    suggestions.add("<number>");
                }
                if (args[0].equalsIgnoreCase("info")){
                    suggestions.addAll(getOnlinePlayers());
                }
            }
        }

        // Filter suggestions based on the current input
        return filterSuggestions(suggestions, args);
    }

    private List<String> filterSuggestions(List<String> suggestions, String[] args) {
        List<String> filtered = new ArrayList<>();
        String currentArg = args[args.length - 1];
        for (String suggestion : suggestions) {
            if (suggestion.toLowerCase().startsWith(currentArg.toLowerCase())) {
                filtered.add(suggestion);
            }
        }
        return filtered;
    }

    public static List<String> getRegionNames(Player player) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(player.getWorld()));

        if (regionManager == null) {
            return Collections.emptyList();
        }

        return new ArrayList<>(regionManager.getRegions().keySet());
    }

    private List<String> getOnlinePlayers() {
        List<String> result = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            result.add(player.getName());
        }
        return result;
    }

}