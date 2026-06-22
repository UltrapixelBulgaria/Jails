package org.proto68.jails.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.proto68.jails.Jails;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseManager {

    private final FileConfiguration config;
    private final Jails plugin;
    private final Logger logger;
    private HikariDataSource dataSource;

    public DatabaseManager(FileConfiguration config, Logger logger, Jails plugin){
        this.config = config;
        this.logger = logger;
        this.plugin = plugin;
    }

    public void connect() throws SQLException {
        HikariConfig hikariConfig = new HikariConfig();

        String url = "jdbc:mysql://" + config.getString("database.address") + "/" + config.getString("database.database") + "?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC";

        hikariConfig.setJdbcUrl(url);
        hikariConfig.setUsername(config.getString("database.username"));
        hikariConfig.setPassword(config.getString("database.password"));
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // --- Pool sizing ---
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(2);

        // --- THIS is what fixes your exact crash ---
        // Validates connections are alive before handing them to your code,
        // and proactively recycles connections before MySQL's wait_timeout kills them
        hikariConfig.setMaxLifetime(1800000);      // 30 min — recycle before MySQL's wait_timeout
        hikariConfig.setIdleTimeout(600000);       // 10 min — close idle connections in the pool
        hikariConfig.setConnectionTimeout(10000);  // 10 sec — fail fast if DB is unreachable
        hikariConfig.setKeepaliveTime(300000);     // 5 min — ping the DB to keep it alive

        hikariConfig.setPoolName("UPBGJail-Pool");

        dataSource = new HikariDataSource(hikariConfig);

        // Test the pool immediately
        try (Connection testConn = dataSource.getConnection()) {
            logger.info("Database connection pool initialized successfully.");
        }

        createTableIfNotExists();
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void createTableIfNotExists() throws SQLException {
        String tableName = config.getString("database.table");
        if (!doesTableExist(tableName)) {
            String query = "CREATE TABLE " + tableName + "(\n" +
                    "\tid INT PRIMARY KEY AUTO_INCREMENT,\n" +
                    "    uuid VARCHAR(36) NOT NULL,\n" +
                    "    username VARCHAR(128) NOT NULL,\n" +
                    "    ip VARCHAR(45),\n" +
                    "    reason VARCHAR(2048),\n" +
                    "    jailed_by_uuid VARCHAR(36),\n" +
                    "    jailed_by_name VARCHAR(128) NOT NULL,\n" +
                    "    unjailed_by_uuid VARCHAR(36),\n" +
                    "    unjailed_by_name VARCHAR(128),\n" +
                    "    unjailed_by_reason VARCHAR(2048),\n" +
                    "    unjailed_by_date DATETIME,\n" +
                    "    cell INTEGER NOT NULL,\n" +
                    "    time BIGINT NOT NULL,\n" +
                    "    until BIGINT NOT NULL,\n" +
                    "    silent BOOLEAN DEFAULT FALSE,\n" +
                    "    active INTEGER NOT NULL DEFAULT 1,\n" +
                    "    in_jail BOOLEAN NOT NULL \n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;";
            String query2 = "ALTER DATABASE " + config.getString("database.database") + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;";
            String query3 = "SET GLOBAL event_scheduler = ON;";
            try (Connection conn = getConnection();
                 Statement statement = conn.createStatement()) {
                statement.executeUpdate(query);
                statement.executeUpdate(query2);
                statement.executeUpdate(query3);
                logger.info("Database table was created!");
            } catch (SQLException e) {
                logger.severe("Error creating table: " + e.getMessage());
                throw e;
            }
        } else {
            logger.info("Table already exists.");
        }
    }



    private boolean doesTableExist(String tableName) {
        String sql = "SHOW TABLES LIKE ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName);

            ResultSet rs = ps.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error while checking if the table exists", e);
        }

        return false;
    }


    public void jailPlayerAsync(UUID uuid, String username, String ip,
                                UUID jailedByUUID, String jailedByName,
                                String reason, int cell, long durationSeconds, boolean silent) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> jailPlayer(uuid, username, ip, jailedByUUID, jailedByName, reason, cell, durationSeconds, silent));
    }

    public void jailPlayer(UUID uuid, String username, String ip,
                           UUID jailedByUUID, String jailedByName,
                           String reason, int cell, long durationSeconds, boolean silent) {

        long now = System.currentTimeMillis();
        long until = now + (durationSeconds * 1000L);

        Player player = Bukkit.getPlayer(uuid);

        boolean inJail = player != null; // if not online -> false


        String sql = "INSERT INTO " + config.getString("database.table") + " " +
                "(uuid, username, ip, reason, jailed_by_uuid, jailed_by_name, cell, time, until, active, in_jail, silent) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, uuid.toString());
            ps.setString(2, username);
            ps.setString(3, ip);
            ps.setString(4, reason);
            ps.setString(5, jailedByUUID != null ? jailedByUUID.toString() : null);
            ps.setString(6, jailedByName);
            ps.setInt(7, cell);
            ps.setLong(8, durationSeconds);
            ps.setLong(9, until);
            ps.setBoolean(10, inJail);
            ps.setBoolean(11, silent);

            ps.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error while jailing the player", e);
        }
    }

    public void updateInJail(UUID uuid, boolean inJailValue, int activeValue) {
        String sql = "UPDATE " + config.getString("database.table") + " SET in_jail = ? WHERE uuid = ? AND active = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, inJailValue);
            ps.setString(2, uuid.toString());
            ps.setInt(3, activeValue);

            ps.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error while updating player info", e);
        }
    }


    public void unjailPlayer(UUID uuid,
                             UUID unjailedByUUID,
                             String unjailedByName,
                             String reason) {

        Player player = Bukkit.getPlayer(uuid);

        boolean online = player != null;
        String text = "";
        if (online) {
            text = "in_jail = 0, ";
        }

        String sql = "UPDATE " + config.getString("database.table") + " SET " +
                "active = 0, " +
                text +
                "unjailed_by_uuid = ?, " +
                "unjailed_by_name = ?, " +
                "unjailed_by_reason = ?, " +
                "unjailed_by_date = NOW() " +
                "WHERE uuid = ? AND active = 1";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, unjailedByUUID != null ? unjailedByUUID.toString() : null);
            ps.setString(2, unjailedByName);
            ps.setString(3, reason);
            ps.setString(4, uuid.toString());

            ps.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error while unjailing the player", e);
        }
    }

    public boolean isJailed(UUID uuid) {

        String sql = "SELECT until FROM " + config.getString("database.table") + " " +
                "WHERE uuid = ? AND active = 1 " +
                "ORDER BY id DESC LIMIT 1";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, uuid.toString());

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                long until = rs.getLong("until");
                return System.currentTimeMillis() < until;
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error while checking if the player is jailed", e);
        }

        return false;
    }


    // Returns usernames of all currently active jailed players
    public Set<String> getActiveJailedUsernames() {
        Set<String> names = new HashSet<>();
        String sql = "SELECT username FROM " + config.getString("database.table") +
                " WHERE active = 1 AND until > ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, System.currentTimeMillis());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                names.add(rs.getString("username"));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error while getting jailed players usernames", e);
        }

        return names;
    }

    public JailRecord getJailRecordByCell(int cell) {
        String sql = "SELECT * FROM " + config.getString("database.table") +
                " WHERE cell = ? AND active = 1 AND until > ? ORDER BY id DESC LIMIT 1";

        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cell);
            ps.setLong(2, System.currentTimeMillis());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return new JailRecord(rs);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error while getting the jail record by cell", e);
        }

        return null;
    }

    /**
     * Fetches the latest jail record for a player.
     * Returns null if no record exists.
     */
    public JailRecord getJailRecord(UUID uuid) {
        String sql = "SELECT * FROM " + config.getString("database.table") +
                " WHERE uuid = ? ORDER BY id DESC LIMIT 1";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new JailRecord(rs);
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error while getting the jail record", e);
        }

        return null;
    }

    /**
     * Fetches all jail records for a player (full history).
     */
    public List<JailRecord> getJailHistory(UUID uuid) {
        String sql = "SELECT * FROM " + config.getString("database.table") +
                " WHERE uuid = ? ORDER BY id ASC";

        List<JailRecord> records = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                records.add(new JailRecord(rs));
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error while getting the jail history", e);
        }

        return records;
    }

    public Set<Integer> getActiveCells() {
        Set<Integer> cells = new HashSet<>();

        String sql = "SELECT cell FROM " + config.getString("database.table") + " WHERE active = 1";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                cells.add(rs.getInt("cell"));
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error while getting the active cells", e);
        }

        return cells;
    }

    public void loadAndScheduleJails() {

        String sql = "SELECT uuid, until FROM " + config.getString("database.table") + " WHERE active = 1";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                long until = rs.getLong("until");

                plugin.getJailScheduler().scheduleRelease(uuid, until);
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error while loading jails from DB", e);
        }
    }
}
