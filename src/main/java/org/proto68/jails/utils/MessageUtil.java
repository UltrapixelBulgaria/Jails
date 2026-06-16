package org.proto68.jails.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.proto68.jails.Jails;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final LegacyComponentSerializer LEGACY_SERIALIZER =
            LegacyComponentSerializer.builder()
                    .hexColors()
                    .useUnusualXRepeatedCharacterHexFormat()
                    .build();

    // Matches &#rrggbb hex codes
    private static final Pattern LEGACY_HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");

    // Matches &a, &l, &r, &c, etc. — only valid Bukkit codes
    private static final Pattern LEGACY_AMPERSAND = Pattern.compile("&([0-9a-fk-orA-FK-OR])");

    public static String colorize(String raw) {
        if (raw == null) return "";

        // Step 1 — pull out all &codes and &#hex BEFORE MiniMessage sees the string,
        // replacing them with a safe placeholder MiniMessage won't choke on,
        // then restore them after MiniMessage is done

        // Convert &#rrggbb → <#rrggbb> (MiniMessage native hex — safe to pass in)
        raw = LEGACY_HEX.matcher(raw).replaceAll("<#$1>");

        // Temporarily escape & codes so MiniMessage doesn't see them at all,
        // using a marker that can't appear in normal text
        raw = LEGACY_AMPERSAND.matcher(raw).replaceAll("\u0000$1");

        // Step 2 — run MiniMessage on what's left (<tags> and plain text only)
        Component component = MINI_MESSAGE.deserialize(raw);

        // Step 3 — serialize to § string
        String result = LEGACY_SERIALIZER.serialize(component);

        // Step 4 — restore our escaped & codes as proper § codes NOW
        result = result.replace("\u0000", "§");

        return result;
    }

    public static String get(Jails plugin, String path, String... placeholders) {
        String message = plugin.getConfig()
                .getString("messages." + path, "&cMessage not found: " + path);

        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            message = message.replace("%" + placeholders[i] + "%", placeholders[i + 1]);
        }

        return colorize(message);
    }
}