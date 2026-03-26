package com.agent007;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MemoryStore {
    private Connection conn;

    public MemoryStore() {
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:agent007.db");
            initTables();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void initTables() throws SQLException {
        String messages = """
            CREATE TABLE IF NOT EXISTS messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                chat_id TEXT NOT NULL,
                role TEXT NOT NULL,
                content TEXT NOT NULL,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """;
        
        String facts = """
            CREATE TABLE IF NOT EXISTS facts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                chat_id TEXT NOT NULL,
                fact TEXT NOT NULL,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """;
        
        conn.createStatement().execute(messages);
        conn.createStatement().execute(facts);
    }

    public void saveMessage(String chatId, String role, String content) {
        try {
            PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO messages (chat_id, role, content) VALUES (?, ?, ?)"
            );
            stmt.setString(1, chatId);
            stmt.setString(2, role);
            stmt.setString(3, content);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String buildContext(String chatId) {
        StringBuilder context = new StringBuilder();
        
        List<String> facts = getFacts(chatId);
        if (!facts.isEmpty()) {
            context.append("Known facts:\n");
            facts.forEach(f -> context.append("- ").append(f).append("\n"));
            context.append("\n");
        }
        
        List<String[]> history = getRecentMessages(chatId, 10);
        if (!history.isEmpty()) {
            context.append("Conversation history:\n");
            history.forEach(msg -> context.append(msg[0]).append(": ").append(msg[1]).append("\n"));
        }
        
        return context.toString();
    }

    private List<String> getFacts(String chatId) {
        List<String> facts = new ArrayList<>();
        try {
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT fact FROM facts WHERE chat_id = ? ORDER BY timestamp DESC"
            );
            stmt.setString(1, chatId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                facts.add(rs.getString("fact"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return facts;
    }

    private List<String[]> getRecentMessages(String chatId, int limit) {
        List<String[]> messages = new ArrayList<>();
        try {
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT role, content FROM messages WHERE chat_id = ? ORDER BY timestamp DESC LIMIT ?"
            );
            stmt.setString(1, chatId);
            stmt.setInt(2, limit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                messages.add(0, new String[]{rs.getString("role"), rs.getString("content")});
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }

    public void extractAndSaveFacts(String chatId, String userMessage, String response) {
        if (userMessage.toLowerCase().contains("my name is") || 
            userMessage.toLowerCase().contains("i am") ||
            userMessage.toLowerCase().contains("i like") ||
            userMessage.toLowerCase().contains("i prefer")) {
            saveFact(chatId, userMessage);
        }
    }

    private void saveFact(String chatId, String fact) {
        try {
            PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO facts (chat_id, fact) VALUES (?, ?)"
            );
            stmt.setString(1, chatId);
            stmt.setString(2, fact);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
