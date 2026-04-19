package org.proto68.jails;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.proto68.jails.database.DatabaseManager;
import org.proto68.jails.database.JailRecord;

public class JailPlaceholderExpansion extends PlaceholderExpansion {

    private final Jails plugin;

    public JailPlaceholderExpansion(Jails plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "jail";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        // Stay registered through PAPI reloads
        return true;
    }

    /*
     * Available placeholders:
     *
     * Cell-based (replace N with cell number):
     *   %jail_player_N%        → username of player in cell N
     *   %jail_reason_N%        → jail reason for cell N
     *   %jail_time_left_N%     → formatted time remaining for cell N
     *   %jail_jailed_by_N%     → who jailed the player in cell N
     *   %jail_occupied_N%      → "Yes" / "No" — whether cell N is occupied
     *
     * Player-based (requires a player context):
     *   %jail_is_jailed%       → "Yes" / "No"
     *   %jail_my_cell%         → cell number the player is in
     *   %jail_my_reason%       → this player's jail reason
     *   %jail_my_time_left%    → this player's time remaining
     *   %jail_my_jailed_by%    → who jailed this player
     */
    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        DatabaseManager db = plugin.getDatabaseManager();
        if (db == null) return "N/A";

        // --- Player-based placeholders ---
        if (player != null) {
            switch (params) {
                case "is_jailed" -> {
                    JailRecord record = db.getJailRecord(player.getUniqueId());
                    return isActive(record) ? "Yes" : "No";
                }
                case "my_cell" -> {
                    JailRecord record = db.getJailRecord(player.getUniqueId());
                    return isActive(record) ? String.valueOf(record.cell) : "None";
                }
                case "my_reason" -> {
                    JailRecord record = db.getJailRecord(player.getUniqueId());
                    return isActive(record) ? orDefault(record.reason) : "None";
                }
                case "my_time_left" -> {
                    JailRecord record = db.getJailRecord(player.getUniqueId());
                    return isActive(record) ? formatTimeLeft(record.until) : "None";
                }
                case "my_jailed_by" -> {
                    JailRecord record = db.getJailRecord(player.getUniqueId());
                    return isActive(record) ? record.jailedByName : "None";
                }
            }
        }

        // --- Cell-based placeholders: jail_<type>_<cellNumber> ---
        String[] parts = params.split("_(?=\\d+$)"); // split on last underscore before digits
        if (parts.length != 2) return null;

        String type = parts[0];
        int cell;
        try {
            cell = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }

        JailRecord record = db.getJailRecordByCell(cell);

        return switch (type) {
            case "occupied"  -> isActive(record) ? "Yes" : "No";
            case "player"    -> isActive(record) ? record.username       : "Empty";
            case "reason"    -> isActive(record) ? orDefault(record.reason) : "Empty";
            case "time_left" -> isActive(record) ? formatTimeLeft(record.until) : "Empty";
            case "jailed_by" -> isActive(record) ? record.jailedByName  : "Empty";
            default          -> null;
        };
    }

    // --- Helpers ---

    private boolean isActive(JailRecord record) {
        return record != null && record.active == 1 && System.currentTimeMillis() < record.until;
    }

    private String orDefault(String value) {
        return (value != null && !value.isBlank()) ? value : "No reason provided";
    }

    private String formatTimeLeft(long until) {
        long ms = until - System.currentTimeMillis();
        if (ms <= 0) return "Expired";

        long seconds = ms / 1000;
        long days    = seconds / 86400; seconds %= 86400;
        long hours   = seconds / 3600;  seconds %= 3600;
        long minutes = seconds / 60;    seconds %= 60;

        StringBuilder sb = new StringBuilder();
        if (days    > 0) sb.append(days).append("d ");
        if (hours   > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.isEmpty()) sb.append(seconds).append("s");

        return sb.toString().trim();
    }
}