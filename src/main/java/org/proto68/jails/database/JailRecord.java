package org.proto68.jails.database;

import java.sql.ResultSet;
import java.sql.SQLException;

// JailRecord.java - Data class to hold all jail info
public class JailRecord {
    public final int id;
    public final String uuid;
    public final String username;
    public final String ip;
    public final String reason;
    public final String jailedByUuid;
    public final String jailedByName;
    public final String unjailedByUuid;
    public final String unjailedByName;
    public final String unjailedByReason;
    public final java.sql.Timestamp unjailedByDate;
    public final int cell;
    public final long time;
    public final long until;
    public final boolean silent;
    public final int active;
    public final boolean inJail;

    public JailRecord(ResultSet rs) throws SQLException {
        this.id             = rs.getInt("id");
        this.uuid           = rs.getString("uuid");
        this.username       = rs.getString("username");
        this.ip             = rs.getString("ip");
        this.reason         = rs.getString("reason");
        this.jailedByUuid   = rs.getString("jailed_by_uuid");
        this.jailedByName   = rs.getString("jailed_by_name");
        this.unjailedByUuid = rs.getString("unjailed_by_uuid");
        this.unjailedByName = rs.getString("unjailed_by_name");
        this.unjailedByReason = rs.getString("unjailed_by_reason");
        this.unjailedByDate = rs.getTimestamp("unjailed_by_date");
        this.cell           = rs.getInt("cell");
        this.time           = rs.getLong("time");
        this.until          = rs.getLong("until");
        this.silent         = rs.getBoolean("silent");
        this.active         = rs.getInt("active");
        this.inJail         = rs.getBoolean("in_jail");
    }

    // Computed helpers
    public boolean isJailed() {
        return active == 1 && System.currentTimeMillis() < until;
    }

}
