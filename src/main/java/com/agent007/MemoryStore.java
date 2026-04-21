package com.agent007;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MemoryStore {
    private Connection conn;
    private static final String DB_URL = "jdbc:sqlite:agent007.db";
    private OllamaClient ollamaClient;

    public MemoryStore() {
        try {
            initConnection();
            initTables();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setOllamaClient(OllamaClient client) {
        this.ollamaClient = client;
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
            CREATE TABLE IF NOT EXISTS messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                chat_id TEXT NOT NULL,
                role TEXT NOT NULL,
                content TEXT NOT NULL,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """);
        getConn().createStatement().execute("""
            CREATE TABLE IF NOT EXISTS facts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                chat_id TEXT NOT NULL,
                fact TEXT NOT NULL,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """);
    }

    public void saveMessage(String chatId, String role, String content) {
        try (PreparedStatement stmt = getConn().prepareStatement(
                "INSERT INTO messages (chat_id, role, content) VALUES (?, ?, ?)")) {
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
            context.append("Known facts about the user:\n");
            facts.forEach(f -> context.append("- ").append(f).append("\n"));
            context.append("\n");
        }
        List<String[]> history = getRecentMessages(chatId, 10);
        if (!history.isEmpty()) {
            context.append("Recent conversation:\n");
            history.forEach(msg -> context.append(msg[0]).append(": ").append(msg[1]).append("\n"));
        }
        return context.toString();
    }

    private List<String> getFacts(String chatId) {
        List<String> facts = new ArrayList<>();
        try (PreparedStatement stmt = getConn().prepareStatement(
                "SELECT fact FROM facts WHERE chat_id = ? ORDER BY timestamp DESC LIMIT 20")) {
            stmt.setString(1, chatId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) facts.add(rs.getString("fact"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return facts;
    }

    private List<String[]> getRecentMessages(String chatId, int limit) {
        List<String[]> messages = new ArrayList<>();
        try (PreparedStatement stmt = getConn().prepareStatement(
                "SELECT role, content FROM messages WHERE chat_id = ? ORDER BY timestamp DESC LIMIT ?")) {
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
        if (ollamaClient != null) {
            extractFactsWithOllama(chatId, userMessage);
        } else {
            extractFactsWithKeywords(chatId, userMessage);
        }
    }

    private void extractFactsWithOllama(String chatId, String userMessage) {
        String prompt = String.format("""
            Extract any personal facts from this user message that are worth remembering long-term (name, preferences, job, location, interests, etc.).
            Reply with a single line containing the fact, or reply with "NONE" if there is nothing worth saving.
            Do not include temporary or task-related info.

            User message: "%s"
            """, userMessage);

        try {
            String extracted = ollamaClient.chat("", prompt).trim();
            if (!extracted.isBlank() && !extracted.equalsIgnoreCase("NONE")
                    && !extracted.toLowerCase().contains("none")) {
                saveFact(chatId, extracted);
            }
        } catch (Exception e) {
            extractFactsWithKeywords(chatId, userMessage);
        }
    }

    private void extractFactsWithKeywords(String chatId, String userMessage) {
        String lower = userMessage.toLowerCase();
        if (lower.contains("my name is") || lower.contains("i am") ||
                lower.contains("i like") || lower.contains("i prefer") ||
                lower.contains("i work") || lower.contains("i live") ||
                lower.contains("i'm a") || lower.contains("i love") ||
                lower.contains("i hate") || lower.contains("my job")) {
            saveFact(chatId, userMessage);
        }
    }

    private void saveFact(String chatId, String fact) {
        try (PreparedStatement stmt = getConn().prepareStatement(
                "INSERT INTO facts (chat_id, fact) VALUES (?, ?)")) {
            stmt.setString(1, chatId);
            stmt.setString(2, fact);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
