package com.agent007;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserPreferences {
    private Connection conn;
    private static final String DB_URL = "jdbc:sqlite:agent007.db";

    public UserPreferences() {
        try {
            initConnection();
            initTables();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void initConnection() throws SQLException {
        conn = DriverManager.getConnection(DB_URL);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA busy_timeout=5000");
        }
    }

    private Connection getConn() {
        try {
            if (conn == null || conn.isClosed()) {
                initConnection();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conn;
    }

    private void initTables() throws SQLException {
        getConn().createStatement().execute("""
            CREATE TABLE IF NOT EXISTS user_preferences (
                chat_id TEXT PRIMARY KEY,
                location TEXT,
                weather_enabled BOOLEAN DEFAULT 1,
                hourly_weather BOOLEAN DEFAULT 1,
                daily_weather BOOLEAN DEFAULT 1,
                daily_weather_time TEXT DEFAULT '08:00'
            )
        """);
    }

    private void upsert(String chatId, String column, Object value) {
        String sql = String.format("""
            INSERT INTO user_preferences (chat_id, %s)
            VALUES (?, ?)
            ON CONFLICT(chat_id) DO UPDATE SET %s = ?
        """, column, column);
        try (PreparedStatement pstmt = getConn().prepareStatement(sql)) {
            pstmt.setString(1, chatId);
            if (value instanceof Boolean b) {
                pstmt.setBoolean(2, b);
                pstmt.setBoolean(3, b);
            } else {
                pstmt.setObject(2, value);
                pstmt.setObject(3, value);
            }
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setLocation(String chatId, String location) {
        upsert(chatId, "location", location);
    }

    public String getLocation(String chatId) {
        try (PreparedStatement pstmt = getConn().prepareStatement(
                "SELECT location FROM user_preferences WHERE chat_id = ?")) {
            pstmt.setString(1, chatId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String loc = rs.getString("location");
                return loc != null ? loc : "Atlanta";
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        setLocation(chatId, "Atlanta");
        return "Atlanta";
    }

    public void setWeatherEnabled(String chatId, boolean enabled) {
        upsert(chatId, "weather_enabled", enabled);
    }

    public boolean isWeatherEnabled(String chatId) {
        try (PreparedStatement pstmt = getConn().prepareStatement(
                "SELECT weather_enabled FROM user_preferences WHERE chat_id = ?")) {
            pstmt.setString(1, chatId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getBoolean("weather_enabled");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    public void setHourlyWeather(String chatId, boolean enabled) {
        upsert(chatId, "hourly_weather", enabled);
    }

    public boolean isHourlyWeatherEnabled(String chatId) {
        try (PreparedStatement pstmt = getConn().prepareStatement(
                "SELECT hourly_weather FROM user_preferences WHERE chat_id = ?")) {
            pstmt.setString(1, chatId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getBoolean("hourly_weather");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    public List<String> getAllChatIdsWithWeatherEnabled() {
        List<String> chatIds = new ArrayList<>();
        try (Statement stmt = getConn().createStatement();
             ResultSet rs = stmt.executeQuery("""
                 SELECT DISTINCT chat_id FROM user_preferences
                 WHERE location IS NOT NULL
                   AND (weather_enabled IS NULL OR weather_enabled = 1)
             """)) {
            while (rs.next()) chatIds.add(rs.getString("chat_id"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return chatIds;
    }

    public List<String> getAllActiveChatIds() {
        List<String> chatIds = new ArrayList<>();
        try (Statement stmt = getConn().createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT DISTINCT chat_id FROM user_preferences WHERE location IS NOT NULL")) {
            while (rs.next()) chatIds.add(rs.getString("chat_id"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return chatIds;
    }
}
