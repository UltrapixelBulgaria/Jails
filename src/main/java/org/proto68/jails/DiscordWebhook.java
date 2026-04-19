package org.proto68.jails;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DiscordWebhook {

    private final Jails plugin;

    public DiscordWebhook(Jails plugin) {
        this.plugin = plugin;
    }

    public enum Event {
        JAIL("jail"),
        UNJAIL("unjail"),
        AUTO_RELEASE("auto_release");

        public final String configKey;
        Event(String configKey) { this.configKey = configKey; }
    }

    public void send(Event event, Map<String, String> placeholders) {
        ConfigurationSection discord = plugin.getConfig().getConfigurationSection("discord");
        if (discord == null || !discord.getBoolean("enabled")) return;

        ConfigurationSection section = discord.getConfigurationSection(event.configKey);
        if (section == null || !section.getBoolean("enabled")) return;

        String webhookUrl = discord.getString("webhook_url", "");
        if (webhookUrl.isBlank() || webhookUrl.contains("YOUR")) {
            plugin.getLogger().warning("Discord webhook URL is not configured.");
            return;
        }

        String payload = buildPayload(section, placeholders);

        // Send async — never block the main thread for HTTP
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL(webhookUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agent", "UPBGJail");
                connection.setDoOutput(true);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(payload.getBytes(StandardCharsets.UTF_8));
                }

                int code = connection.getResponseCode();
                if (code != 200 && code != 204) {
                    plugin.getLogger().warning("Discord webhook returned HTTP " + code);
                }

                connection.disconnect();
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to send Discord webhook: " + e.getMessage());
            }
        });
    }

    private String buildPayload(ConfigurationSection section, Map<String, String> placeholders) {
        String title     = resolve(section.getString("title", ""),     placeholders);
        String footer    = resolve(section.getString("footer", ""),    placeholders);
        String thumbnail = resolve(section.getString("thumbnail", ""), placeholders);
        int color        = section.getInt("color", 0);

        StringBuilder fields = new StringBuilder();
        List<Map<?, ?>> configFields = section.getMapList("fields");

        for (Map<?, ?> field : configFields) {
            String name   = resolve(String.valueOf(field.get("name")),  placeholders);
            String value  = resolve(String.valueOf(field.get("value")), placeholders);
            boolean inline = Boolean.parseBoolean(String.valueOf(field.get("inline")));

            if (!fields.isEmpty()) fields.append(",");
            fields.append(String.format(
                    "{\"name\":\"%s\",\"value\":\"%s\",\"inline\":%b}",
                    escapeJson(name), escapeJson(value), inline
            ));
        }

        String thumbnailJson = thumbnail.isBlank() ? "" :
                String.format(",\"thumbnail\":{\"url\":\"%s\"}", escapeJson(thumbnail));

        return String.format("""
                {
                  "embeds": [{
                    "title": "%s",
                    "color": %d,
                    "fields": [%s],
                    "footer": {"text": "%s"},
                    "timestamp": "%s"%s
                  }]
                }
                """,
                escapeJson(title),
                color,
                fields,
                escapeJson(footer),
                Instant.now().toString(),
                thumbnailJson
        );
    }

    private String resolve(String text, Map<String, String> placeholders) {
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            text = text.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return text;
    }

    private String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    // --- Convenience builders so call sites are clean ---

    public void sendJail(String player, String staff, String cell, String time, String reason) {
        Map<String, String> ph = new HashMap<>();
        ph.put("player", player);
        ph.put("staff",  staff);
        ph.put("cell",   cell);
        ph.put("time",   time);
        ph.put("reason", reason != null ? reason : "No reason provided");
        send(Event.JAIL, ph);
    }

    public void sendUnjail(String player, String staff, String reason) {
        Map<String, String> ph = new HashMap<>();
        ph.put("player", player);
        ph.put("staff",  staff);
        ph.put("reason", reason != null ? reason : "No reason provided");
        send(Event.UNJAIL, ph);
    }

    public void sendAutoRelease(String player) {
        Map<String, String> ph = new HashMap<>();
        ph.put("player", player);
        send(Event.AUTO_RELEASE, ph);
    }
}