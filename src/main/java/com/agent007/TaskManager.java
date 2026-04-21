package com.agent007;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TaskManager {
    private Connection conn;
    private static final String DB_URL = "jdbc:sqlite:agent007.db";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("MMM dd 'at' h:mm a");

    public TaskManager() {
        try {
            initConnection();
            initTables();
            migrateDatabase();
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
        try (Statement stmt = getConn().createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS tasks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    chat_id TEXT NOT NULL,
                    task TEXT NOT NULL,
                    completed BOOLEAN DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS reminders (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    chat_id TEXT NOT NULL,
                    message TEXT NOT NULL,
                    remind_at TIMESTAMP NOT NULL,
                    sent BOOLEAN DEFAULT 0,
                    recurrence TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS notes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    chat_id TEXT NOT NULL,
                    title TEXT,
                    content TEXT NOT NULL,
                    tags TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS stocks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    chat_id TEXT NOT NULL,
                    symbol TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE(chat_id, symbol)
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS expenses (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    chat_id TEXT NOT NULL,
                    amount REAL NOT NULL,
                    description TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS stock_alerts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    chat_id TEXT NOT NULL,
                    symbol TEXT NOT NULL,
                    direction TEXT NOT NULL,
                    target_price REAL NOT NULL,
                    triggered BOOLEAN DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
        }
    }

    private void migrateDatabase() {
        // Add recurrence to existing reminders table
        try {
            getConn().createStatement().execute("ALTER TABLE reminders ADD COLUMN recurrence TEXT");
        } catch (SQLException ignored) {}
    }

    // ── Tasks ────────────────────────────────────────────────────────────────

    public void addTask(String chatId, String task) {
        try (PreparedStatement pstmt = getConn().prepareStatement(
                "INSERT INTO tasks (chat_id, task) VALUES (?, ?)")) {
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
        try (PreparedStatement pstmt = getConn().prepareStatement(sql)) {
            pstmt.setString(1, chatId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String status = rs.getBoolean("completed") ? "✅" : "⬜";
                tasks.add(String.format("%s [%d] %s", status, rs.getInt("id"), rs.getString("task")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tasks;
    }

    public boolean completeTask(String chatId, int taskId) {
        try (PreparedStatement pstmt = getConn().prepareStatement(
                "UPDATE tasks SET completed = 1 WHERE id = ? AND chat_id = ?")) {
            pstmt.setInt(1, taskId);
            pstmt.setString(2, chatId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteTask(String chatId, int taskId) {
        try (PreparedStatement pstmt = getConn().prepareStatement(
                "DELETE FROM tasks WHERE id = ? AND chat_id = ?")) {
            pstmt.setInt(1, taskId);
            pstmt.setString(2, chatId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ── Reminders ────────────────────────────────────────────────────────────

    public void addReminder(String chatId, String message, LocalDateTime remindAt) {
        addReminder(chatId, message, remindAt, null);
    }

    public void addReminder(String chatId, String message, LocalDateTime remindAt, String recurrence) {
        try (PreparedStatement pstmt = getConn().prepareStatement(
                "INSERT INTO reminders (chat_id, message, remind_at, recurrence) VALUES (?, ?, ?, ?)")) {
            pstmt.setString(1, chatId);
            pstmt.setString(2, message);
            pstmt.setString(3, remindAt.format(formatter));
            pstmt.setString(4, recurrence);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Reminder> getPendingReminders() {
        List<Reminder> reminders = new ArrayList<>();
        String sql = """
            SELECT id, chat_id, message, recurrence FROM reminders
            WHERE sent = 0
              AND remind_at <= datetime('now')
              AND remind_at >= datetime('now', '-60 seconds')
            ORDER BY remind_at
        """;
        try (Statement stmt = getConn().createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                reminders.add(new Reminder(
                        rs.getInt("id"),
                        rs.getString("chat_id"),
                        rs.getString("message"),
                        rs.getString("recurrence")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reminders;
    }

    public void markReminderSent(int reminderId) {
        try (PreparedStatement pstmt = getConn().prepareStatement(
                "UPDATE reminders SET sent = 1 WHERE id = ?")) {
            pstmt.setInt(1, reminderId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getReminderMessage(int reminderId) {
        try (PreparedStatement pstmt = getConn().prepareStatement(
                "SELECT message FROM reminders WHERE id = ?")) {
            pstmt.setInt(1, reminderId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getString("message");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<String> getUpcomingReminders(String chatId) {
        List<String> list = new ArrayList<>();
        try (PreparedStatement pstmt = getConn().prepareStatement(
                "SELECT id, message, remind_at, recurrence FROM reminders WHERE chat_id = ? AND sent = 0 ORDER BY remind_at")) {
            pstmt.setString(1, chatId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                LocalDateTime dt = LocalDateTime.parse(rs.getString("remind_at"), formatter);
                String recurrence = rs.getString("recurrence");
                String recStr = recurrence != null ? " (" + recurrence + ")" : "";
                list.add(String.format("[%d] %s — %s%s",
                        rs.getInt("id"), dt.format(displayFormatter), rs.getString("message"), recStr));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean deleteReminder(String chatId, int reminderId) {
        try (PreparedStatement pstmt = getConn().prepareStatement(
                "DELETE FROM reminders WHERE id = ? AND chat_id = ?")) {
            pstmt.setInt(1, reminderId);
            pstmt.setString(2, chatId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ── Notes ────────────────────────────────────────────────────────────────

    public void addNote(String chatId, String title, String content, String tags) {
        try (PreparedStatement pstmt = getConn().prepareStatement(
                "INSERT INTO notes (chat_id, title, content, tags) VALUES (?, ?, ?, ?)")) {
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
        try (PreparedStatement pstmt = getConn().prepareStatement(sql)) {
            pstmt.setString(1, chatId);
            if (tag != null) pstmt.setString(2, "%" + tag + "%");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                notes.add(String.format("📝 [%d] %s\n%s\nTags: %s",
                        rs.getInt("id"), rs.getString("title"),
                        rs.getString("content"), rs.getString("tags")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return notes;
    }

    public List<String> searchNotes(String chatId, String keyword) {
        List<String> notes = new ArrayList<>();
        String sql = """
            SELECT id, title, content, tags FROM notes
            WHERE chat_id = ? AND (title LIKE ? OR content LIKE ? OR tags LIKE ?)
            ORDER BY created_at DESC
        """;
        try (PreparedStatement pstmt = getConn().prepareStatement(sql)) {
            String like = "%" + keyword + "%";
            pstmt.setString(1, chatId);
            pstmt.setString(2, like);
            pstmt.setString(3, like);
            pstmt.setString(4, like);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                notes.add(String.format("📝 [%d] %s\n%s\nTags: %s",
                        rs.getInt("id"), rs.getString("title"),
                        rs.getString("content"), rs.getString("tags")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return notes;
    }

    public boolean deleteNote(String chatId, int noteId) {
        try (PreparedStatement pstmt = getConn().prepareStatement(
                "DELETE FROM notes WHERE id = ? AND chat_id = ?")) {
            pstmt.setInt(1, noteId);
            pstmt.setString(2, chatId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ── Stocks ───────────────────────────────────────────────────────────────

    public void addStock(String chatId, String symbol) {
        try (PreparedStatement pstmt = getConn().prepareStatement(
                "INSERT OR IGNORE INTO stocks (chat_id, symbol) VALUES (?, ?)")) {
            pstmt.setString(1, chatId);
            pstmt.setString(2, symbol.toUpperCase());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String> getStocks(String chatId) {
        List<String> stocks = new ArrayList<>();
        try (PreparedStatement pstmt = getConn().prepareStatement(
                "SELECT symbol FROM stocks WHERE chat_id = ? ORDER BY symbol")) {
            pstmt.setString(1, chatId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) stocks.add(rs.getString("symbol"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stocks;
    }

    public boolean deleteStock(String chatId, String symbol) {
        try (PreparedStatement pstmt = getConn().prepareStatement(
                "DELETE FROM stocks WHERE chat_id = ? AND symbol = ?")) {
            pstmt.setString(1, chatId);
            pstmt.setString(2, symbol.toUpperCase());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void initializeDefaultStocks(String chatId) {
        if (getStocks(chatId).isEmpty()) {
            addStock(chatId, "RBLX");
            addStock(chatId, "TEAM");
            addStock(chatId, "AMZN");
            addStock(chatId, "NVDA");
        }
    }

    // ── Stock Alerts ─────────────────────────────────────────────────────────

    public void addStockAlert(String chatId, String symbol, String direction, double targetPrice) {
        try (PreparedStatement pstmt = getConn().prepareStatement(
                "INSERT INTO stock_alerts (chat_id, symbol, direction, target_price) VALUES (?, ?, ?, ?)")) {
            pstmt.setString(1, chatId);
            pstmt.setString(2, symbol.toUpperCase());
            pstmt.setString(3, direction);
            pstmt.setDouble(4, targetPrice);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<StockAlert> getActiveAlerts(String chatId) {
        List<StockAlert> alerts = new ArrayList<>();
        try (PreparedStatement pstmt = getConn().prepareStatement(
                "SELECT id, symbol, direction, target_price FROM stock_alerts WHERE chat_id = ? AND triggered = 0 ORDER BY symbol")) {
            pstmt.setString(1, chatId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                alerts.add(new StockAlert(
                        rs.getInt("id"), chatId,
                        rs.getString("symbol"),
                        rs.getString("direction"),
                        rs.getDouble("target_price")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return alerts;
    }

    public List<StockAlert> getAllActiveAlerts() {
        List<StockAlert> alerts = new ArrayList<>();
        try (Statement stmt = getConn().createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT id, chat_id, symbol, direction, target_price FROM stock_alerts WHERE triggered = 0")) {
            while (rs.next()) {
                alerts.add(new StockAlert(
                        rs.getInt("id"), rs.getString("chat_id"),
                        rs.getString("symbol"),
                        rs.getString("direction"),
                        rs.getDouble("target_price")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return alerts;
    }

    public void markAlertTriggered(int alertId) {
        try (PreparedStatement pstmt = getConn().prepareStatement(
                "UPDATE stock_alerts SET triggered = 1 WHERE id = ?")) {
            pstmt.setInt(1, alertId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean deleteAlert(String chatId, int alertId) {
        try (PreparedStatement pstmt = getConn().prepareStatement(
                "DELETE FROM stock_alerts WHERE id = ? AND chat_id = ?")) {
            pstmt.setInt(1, alertId);
            pstmt.setString(2, chatId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ── Expenses ─────────────────────────────────────────────────────────────

    public void addExpense(String chatId, double amount, String description) {
        try (PreparedStatement pstmt = getConn().prepareStatement(
                "INSERT INTO expenses (chat_id, amount, description) VALUES (?, ?, ?)")) {
            pstmt.setString(1, chatId);
            pstmt.setDouble(2, amount);
            pstmt.setString(3, description);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String> getExpenses(String chatId, String yearMonth) {
        List<String> expenses = new ArrayList<>();
        String filter = yearMonth != null ? yearMonth : java.time.YearMonth.now().toString();
        String sql = """
            SELECT id, amount, description, created_at FROM expenses
            WHERE chat_id = ? AND strftime('%Y-%m', created_at) = ?
            ORDER BY created_at DESC
        """;
        try (PreparedStatement pstmt = getConn().prepareStatement(sql)) {
            pstmt.setString(1, chatId);
            pstmt.setString(2, filter);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                expenses.add(String.format("[%d] $%.2f — %s",
                        rs.getInt("id"), rs.getDouble("amount"), rs.getString("description")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return expenses;
    }

    public double getMonthlyTotal(String chatId, String yearMonth) {
        String filter = yearMonth != null ? yearMonth : java.time.YearMonth.now().toString();
        try (PreparedStatement pstmt = getConn().prepareStatement(
                "SELECT SUM(amount) FROM expenses WHERE chat_id = ? AND strftime('%Y-%m', created_at) = ?")) {
            pstmt.setString(1, chatId);
            pstmt.setString(2, filter);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public boolean deleteExpense(String chatId, int expenseId) {
        try (PreparedStatement pstmt = getConn().prepareStatement(
                "DELETE FROM expenses WHERE id = ? AND chat_id = ?")) {
            pstmt.setInt(1, expenseId);
            pstmt.setString(2, chatId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ── Inner Classes ─────────────────────────────────────────────────────────

    public static class Reminder {
        public final int id;
        public final String chatId;
        public final String message;
        public final String recurrence;

        public Reminder(int id, String chatId, String message, String recurrence) {
            this.id = id;
            this.chatId = chatId;
            this.message = message;
            this.recurrence = recurrence;
        }
    }

    public static class StockAlert {
        public final int id;
        public final String chatId;
        public final String symbol;
        public final String direction;
        public final double targetPrice;

        public StockAlert(int id, String chatId, String symbol, String direction, double targetPrice) {
            this.id = id;
            this.chatId = chatId;
            this.symbol = symbol;
            this.direction = direction;
            this.targetPrice = targetPrice;
        }
    }
}
