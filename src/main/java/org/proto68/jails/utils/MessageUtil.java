package org.proto68.jails.utils;

import org.bukkit.ChatColor;
import org.proto68.jails.Jails;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("#([A-Fa-f0-9]{6})");

    public static String colorize(String message) {
        if (message == null) return "";

        // Replace HEX colors (#rrggbb → §x§r§r§g§g§b§b)
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder buffer = new StringBuilder();

        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append("§").append(c);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);

        // Replace standard &a, &l, etc.
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    public static String get(Jails plugin, String path, String... placeholders) {
        String message = plugin.getConfig().getString("messages." + path, "&cMessage not found: " + path);
        message = colorize(message);

        // Placeholders passed as key-value pairs: "player", "Steve", "staff", "Admin", ...
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            message = message.replace("%" + placeholders[i] + "%", placeholders[i + 1]);
        }

        return message;
    }
}