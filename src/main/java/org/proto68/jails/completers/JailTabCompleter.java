package org.proto68.jails.completers;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.proto68.jails.Jails;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class JailTabCompleter implements TabCompleter {

    private final Jails plugin;

    public JailTabCompleter(Jails plugin) {
        this.plugin = plugin;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        List<String> suggestions = new ArrayList<>();

        boolean isPlayer = sender instanceof Player p && p.hasPermission(
                command.getName().equalsIgnoreCase("jail") ? "jails.jail" : "jails.unjail"
        );

        // Console always gets suggestions, players only if they have permission
        if (sender instanceof Player && !isPlayer) {
            return suggestions;
        }

        if (command.getName().equalsIgnoreCase("jail")) {
            if (args.length == 1) {
                suggestions.addAll(getUnjailedOnlinePlayers());
            } else if (args.length == 2) {
                suggestions.addAll(Arrays.asList("5m", "1d", "3d"));
            } else if (args.length >= 3) {
                // Suggest -s flag if not already typed
                if (Arrays.stream(args).noneMatch(a -> a.equalsIgnoreCase("-s"))) {
                    suggestions.add("-s");
                }
                if (args.length == 3) suggestions.addAll(getTemplateReasons());
            }
        } else if (command.getName().equalsIgnoreCase("unjail")) {
            if (args.length == 1) {
                suggestions.addAll(getJailedPlayerNames());
            } else if (args.length >= 2) {
                if (Arrays.stream(args).noneMatch(a -> a.equalsIgnoreCase("-s"))) {
                    suggestions.add("-s");
                }
                if (args.length == 2) suggestions.add("<reason>");
            }
        }

        return filterSuggestions(suggestions, args);
    }

    private List<String> filterSuggestions(List<String> suggestions, String[] args) {
        List<String> filtered = new ArrayList<>();
        String currentArg = args[args.length - 1].toLowerCase();
        for (String suggestion : suggestions) {
            if (suggestion.toLowerCase().startsWith(currentArg)) {
                filtered.add(suggestion);
            }
        }
        return filtered;
    }

    // Players who are NOT jailed (valid targets for /jail)
    private List<String> getUnjailedOnlinePlayers() {
        Set<String> jailedNames = plugin.getDatabaseManager().getActiveJailedUsernames();
        List<String> result = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.hasPermission("jails.jail") && !jailedNames.contains(player.getName())) {
                result.add(player.getName());
            }
        }
        return result;
    }

    // All currently jailed players (online or offline)
    private List<String> getJailedPlayerNames() {
        return new ArrayList<>(plugin.getDatabaseManager().getActiveJailedUsernames());
    }

    public List<String> getTemplateReasons() {
        return plugin.getConfig().getStringList("template-reasons");
    }
}