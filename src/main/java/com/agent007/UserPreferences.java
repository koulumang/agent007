package com.agent007;

import java.sql.*;

public class UserPreferences {
    private Connection conn;

    public UserPreferences() {
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:agent007.db");
            initTables();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void initTables() throws SQLException {
        String createPrefsTable = """
            CREATE TABLE IF NOT EXISTS user_preferences (
                chat_id TEXT PRIMARY KEY,
                location TEXT,
                weather_enabled BOOLEAN DEFAULT 1,
                hourly_weather BOOLEAN DEFAULT 1,
                daily_weather BOOLEAN DEFAULT 1,
                daily_weather_time TEXT DEFAULT '08:00'
            )
        """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createPrefsTable);
        }
    }

    public void setLocation(String chatId, String location) {
        String sql = """
            INSERT INTO user_preferences (chat_id, location) 
            VALUES (?, ?)
            ON CONFLICT(chat_id) DO UPDATE SET location = ?
        """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, chatId);
            pstmt.setString(2, location);
            pstmt.setString(3, location);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getLocation(String chatId) {
        String sql = "SELECT location FROM user_preferences WHERE chat_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, chatId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String location = rs.getString("location");
                return location != null ? location : "Atlanta";
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        // Default location for new users
        setLocation(chatId, "Atlanta");
        return "Atlanta";
    }

    public void setWeatherEnabled(String chatId, boolean enabled) {
        String sql = """
            INSERT INTO user_preferences (chat_id, weather_enabled) 
            VALUES (?, ?)
            ON CONFLICT(chat_id) DO UPDATE SET weather_enabled = ?
        """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, chatId);
            pstmt.setBoolean(2, enabled);
            pstmt.setBoolean(3, enabled);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isWeatherEnabled(String chatId) {
        String sql = "SELECT weather_enabled FROM user_preferences WHERE chat_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, chatId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("weather_enabled");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true; // Default enabled
    }

    public void setHourlyWeather(String chatId, boolean enabled) {
        String sql = """
            INSERT INTO user_preferences (chat_id, hourly_weather) 
            VALUES (?, ?)
            ON CONFLICT(chat_id) DO UPDATE SET hourly_weather = ?
        """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, chatId);
            pstmt.setBoolean(2, enabled);
            pstmt.setBoolean(3, enabled);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isHourlyWeatherEnabled(String chatId) {
        String sql = "SELECT hourly_weather FROM user_preferences WHERE chat_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, chatId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("hourly_weather");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true; // Default enabled
    }

    public java.util.List<String> getAllChatIdsWithWeatherEnabled() {
        java.util.List<String> chatIds = new java.util.ArrayList<>();
        // Get all chat IDs that have interacted (have a location set, even if default)
        // Weather is enabled by default
        String sql = """
            SELECT DISTINCT chat_id FROM user_preferences 
            WHERE location IS NOT NULL 
            AND (weather_enabled IS NULL OR weather_enabled = 1)
        """;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                chatIds.add(rs.getString("chat_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return chatIds;
    }
}
