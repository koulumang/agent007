package com.agent007;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TaskManager {
    private Connection conn;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public TaskManager() {
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:agent007.db");
            initTables();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void initTables() throws SQLException {
        String createTasksTable = """
            CREATE TABLE IF NOT EXISTS tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                chat_id TEXT NOT NULL,
                task TEXT NOT NULL,
                completed BOOLEAN DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;

        String createRemindersTable = """
            CREATE TABLE IF NOT EXISTS reminders (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                chat_id TEXT NOT NULL,
                message TEXT NOT NULL,
                remind_at TIMESTAMP NOT NULL,
                sent BOOLEAN DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;

        String createNotesTable = """
            CREATE TABLE IF NOT EXISTS notes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                chat_id TEXT NOT NULL,
                title TEXT,
                content TEXT NOT NULL,
                tags TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;

        String createStocksTable = """
            CREATE TABLE IF NOT EXISTS stocks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                chat_id TEXT NOT NULL,
                symbol TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(chat_id, symbol)
            )
        """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createTasksTable);
            stmt.execute(createRemindersTable);
            stmt.execute(createNotesTable);
            stmt.execute(createStocksTable);
        }
    }

    // Task Management
    public void addTask(String chatId, String task) {
        String sql = "INSERT INTO tasks (chat_id, task) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, chatId);
            pstmt.setString(2, task);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String> getTasks(String chatId, boolean includeCompleted) {
        List<String> tasks = new ArrayList<>();
        String sql = includeCompleted 
            ? "SELECT id, task, completed FROM tasks WHERE chat_id = ? ORDER BY created_at DESC"
            : "SELECT id, task, completed FROM tasks WHERE chat_id = ? AND completed = 0 ORDER BY created_at DESC";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, chatId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                String task = rs.getString("task");
                boolean completed = rs.getBoolean("completed");
                String status = completed ? "✅" : "⬜";
                tasks.add(String.format("%s [%d] %s", status, id, task));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tasks;
    }

    public boolean completeTask(String chatId, int taskId) {
        String sql = "UPDATE tasks SET completed = 1 WHERE id = ? AND chat_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, taskId);
            pstmt.setString(2, chatId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteTask(String chatId, int taskId) {
        String sql = "DELETE FROM tasks WHERE id = ? AND chat_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, taskId);
            pstmt.setString(2, chatId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Reminder Management
    public void addReminder(String chatId, String message, LocalDateTime remindAt) {
        String sql = "INSERT INTO reminders (chat_id, message, remind_at) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, chatId);
            pstmt.setString(2, message);
            pstmt.setString(3, remindAt.format(formatter));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Reminder> getPendingReminders() {
        List<Reminder> reminders = new ArrayList<>();
        String sql = "SELECT id, chat_id, message FROM reminders WHERE sent = 0 AND remind_at <= datetime('now') ORDER BY remind_at";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                reminders.add(new Reminder(
                    rs.getInt("id"),
                    rs.getString("chat_id"),
                    rs.getString("message")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reminders;
    }

    public void markReminderSent(int reminderId) {
        String sql = "UPDATE reminders SET sent = 1 WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, reminderId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Note Management
    public void addNote(String chatId, String title, String content, String tags) {
        String sql = "INSERT INTO notes (chat_id, title, content, tags) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, chatId);
            pstmt.setString(2, title);
            pstmt.setString(3, content);
            pstmt.setString(4, tags);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String> getNotes(String chatId, String tag) {
        List<String> notes = new ArrayList<>();
        String sql = tag == null 
            ? "SELECT id, title, content, tags FROM notes WHERE chat_id = ? ORDER BY created_at DESC"
            : "SELECT id, title, content, tags FROM notes WHERE chat_id = ? AND tags LIKE ? ORDER BY created_at DESC";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, chatId);
            if (tag != null) {
                pstmt.setString(2, "%" + tag + "%");
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                String title = rs.getString("title");
                String content = rs.getString("content");
                String tags = rs.getString("tags");
                notes.add(String.format("📝 [%d] %s\n%s\nTags: %s", id, title, content, tags));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return notes;
    }

    public boolean deleteNote(String chatId, int noteId) {
        String sql = "DELETE FROM notes WHERE id = ? AND chat_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, noteId);
            pstmt.setString(2, chatId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Stock Management
    public void addStock(String chatId, String symbol) {
        String sql = "INSERT OR IGNORE INTO stocks (chat_id, symbol) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, chatId);
            pstmt.setString(2, symbol.toUpperCase());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String> getStocks(String chatId) {
        List<String> stocks = new ArrayList<>();
        String sql = "SELECT symbol FROM stocks WHERE chat_id = ? ORDER BY symbol";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, chatId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                stocks.add(rs.getString("symbol"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stocks;
    }

    public boolean deleteStock(String chatId, String symbol) {
        String sql = "DELETE FROM stocks WHERE chat_id = ? AND symbol = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, chatId);
            pstmt.setString(2, symbol.toUpperCase());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void initializeDefaultStocks(String chatId) {
        List<String> currentStocks = getStocks(chatId);
        if (currentStocks.isEmpty()) {
            addStock(chatId, "RBLX");
            addStock(chatId, "TEAM");
            addStock(chatId, "AMZN");
            addStock(chatId, "NVDA");
        }
    }

    public static class Reminder {
        public final int id;
        public final String chatId;
        public final String message;

        public Reminder(int id, String chatId, String message) {
            this.id = id;
            this.chatId = chatId;
            this.message = message;
        }
    }
}
