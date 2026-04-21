package com.agent007;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class CommandHandler {
    private final TaskManager taskManager;
    private final WeatherService weatherService;
    private final UserPreferences userPrefs;
    private final StockService stockService;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("MMM dd 'at' h:mm a");

    public CommandHandler(TaskManager taskManager, WeatherService weatherService,
                          UserPreferences userPrefs, StockService stockService) {
        this.taskManager = taskManager;
        this.weatherService = weatherService;
        this.userPrefs = userPrefs;
        this.stockService = stockService;
    }

    public String handleCommand(String chatId, String message) {
        String[] parts = message.split(" ", 2);
        String command = parts[0].toLowerCase();

        return switch (command) {
            case "/help"         -> getHelpMessage();
            case "/addtask"      -> addTask(chatId, parts);
            case "/tasks"        -> getTasksList(chatId, false);
            case "/alltasks"     -> getTasksList(chatId, true);
            case "/done"         -> completeTask(chatId, parts);
            case "/deltask"      -> deleteTask(chatId, parts);
            case "/remind"       -> addReminder(chatId, parts);
            case "/reminders"    -> listReminders(chatId);
            case "/delreminder"  -> deleteReminder(chatId, parts);
            case "/addnote"      -> addNote(chatId, parts);
            case "/notes"        -> getNotes(chatId, parts);
            case "/delnote"      -> deleteNote(chatId, parts);
            case "/search"       -> searchNotes(chatId, parts);
            case "/weather"      -> getWeather(chatId, parts);
            case "/setlocation"  -> setLocation(chatId, parts);
            case "/weatheron"    -> toggleWeather(chatId, true);
            case "/weatheroff"   -> toggleWeather(chatId, false);
            case "/hourlyon"     -> toggleHourly(chatId, true);
            case "/hourlyoff"    -> toggleHourly(chatId, false);
            case "/resetlocation" -> resetLocation(chatId);
            case "/addstock"     -> addStock(chatId, parts);
            case "/stocks"       -> listStocks(chatId);
            case "/delstock"     -> deleteStock(chatId, parts);
            case "/stocknow"     -> getStockUpdate(chatId);
            case "/alert"        -> addAlert(chatId, parts);
            case "/alerts"       -> listAlerts(chatId);
            case "/delalert"     -> deleteAlert(chatId, parts);
            case "/expense"      -> addExpense(chatId, parts);
            case "/expenses"     -> listExpenses(chatId, parts);
            case "/delexpense"   -> deleteExpense(chatId, parts);
            case "/pomodoro"     -> startPomodoro(chatId, parts);
            case "/digest"       -> buildDigest(chatId);
            default              -> null;
        };
    }

    // ── Tasks ────────────────────────────────────────────────────────────────

    private String addTask(String chatId, String[] parts) {
        if (parts.length < 2) return "Usage: /addtask <task description>";
        taskManager.addTask(chatId, parts[1]);
        return "✅ Task added!";
    }

    private String getTasksList(String chatId, boolean includeCompleted) {
        List<String> tasks = taskManager.getTasks(chatId, includeCompleted);
        if (tasks.isEmpty()) return "No tasks found!";
        return "📋 Your Tasks:\n\n" + String.join("\n", tasks);
    }

    private String completeTask(String chatId, String[] parts) {
        if (parts.length < 2) return "Usage: /done <task_id>";
        try {
            return taskManager.completeTask(chatId, Integer.parseInt(parts[1]))
                    ? "✅ Task completed!" : "❌ Task not found";
        } catch (NumberFormatException e) {
            return "❌ Invalid task ID";
        }
    }

    private String deleteTask(String chatId, String[] parts) {
        if (parts.length < 2) return "Usage: /deltask <task_id>";
        try {
            return taskManager.deleteTask(chatId, Integer.parseInt(parts[1]))
                    ? "🗑️ Task deleted!" : "❌ Task not found";
        } catch (NumberFormatException e) {
            return "❌ Invalid task ID";
        }
    }

    // ── Reminders ────────────────────────────────────────────────────────────

    private String addReminder(String chatId, String[] parts) {
        if (parts.length < 2) return reminderHelp();

        String fullMessage = parts[1];
        NaturalReminderParser.ParsedReminder parsed = NaturalReminderParser.parse("remind me " + fullMessage);
        if (parsed != null) {
            if (parsed.dateTime.isBefore(LocalDateTime.now()))
                return "❌ Cannot set reminder in the past!";
            taskManager.addReminder(chatId, parsed.message, parsed.dateTime, parsed.recurrence);
            String recurrStr = parsed.recurrence != null ? " (repeats " + parsed.recurrence + ")" : "";
            return "⏰ Reminder set for " + parsed.dateTime.format(displayFormatter) + recurrStr + "\n📝 " + parsed.message;
        }

        // Fall back to strict format: YYYY-MM-DD HH:MM message
        String[] reminderParts = fullMessage.split(" ", 3);
        if (reminderParts.length < 3)
            return "❌ Couldn't understand that. Try: 'in 30 minutes to call mom'";

        try {
            LocalDateTime remindAt = LocalDateTime.parse(reminderParts[0] + " " + reminderParts[1], formatter);
            if (remindAt.isBefore(LocalDateTime.now())) return "❌ Cannot set reminder in the past!";
            taskManager.addReminder(chatId, reminderParts[2], remindAt);
            return "⏰ Reminder set for " + reminderParts[0] + " " + reminderParts[1];
        } catch (DateTimeParseException e) {
            return "❌ Couldn't understand that. Try: 'in 30 minutes to call mom'";
        }
    }

    private String listReminders(String chatId) {
        List<String> reminders = taskManager.getUpcomingReminders(chatId);
        if (reminders.isEmpty()) return "⏰ No upcoming reminders.";
        return "⏰ Upcoming Reminders:\n\n" + String.join("\n", reminders);
    }

    private String deleteReminder(String chatId, String[] parts) {
        if (parts.length < 2) return "Usage: /delreminder <id>";
        try {
            return taskManager.deleteReminder(chatId, Integer.parseInt(parts[1]))
                    ? "🗑️ Reminder deleted!" : "❌ Reminder not found";
        } catch (NumberFormatException e) {
            return "❌ Invalid ID";
        }
    }

    private String reminderHelp() {
        return """
            ⏰ Reminder Examples:
            • /remind in 30 minutes to call mom
            • /remind tomorrow at 3pm about meeting
            • /remind on monday at 9am about standup
            • /remind every day at 8am to drink water
            • /remind every monday at 9am to standup
            • /remind every week to review code
            """;
    }

    // ── Notes ────────────────────────────────────────────────────────────────

    private String addNote(String chatId, String[] parts) {
        if (parts.length < 2)
            return "Usage: /addnote <title> | <content> | <tags>\nExample: /addnote Meeting | Discuss Q2 goals | work";
        String[] noteParts = parts[1].split("\\|");
        if (noteParts.length < 2) return "❌ Use: /addnote <title> | <content> | <tags>";
        taskManager.addNote(chatId,
                noteParts[0].trim(),
                noteParts[1].trim(),
                noteParts.length > 2 ? noteParts[2].trim() : "");
        return "📝 Note saved!";
    }

    private String getNotes(String chatId, String[] parts) {
        String tag = parts.length > 1 ? parts[1] : null;
        List<String> notes = taskManager.getNotes(chatId, tag);
        if (notes.isEmpty())
            return tag != null ? "No notes with tag: " + tag : "No notes found!";
        return "📝 Your Notes:\n\n" + String.join("\n\n", notes);
    }

    private String deleteNote(String chatId, String[] parts) {
        if (parts.length < 2) return "Usage: /delnote <note_id>";
        try {
            return taskManager.deleteNote(chatId, Integer.parseInt(parts[1]))
                    ? "🗑️ Note deleted!" : "❌ Note not found";
        } catch (NumberFormatException e) {
            return "❌ Invalid ID";
        }
    }

    private String searchNotes(String chatId, String[] parts) {
        if (parts.length < 2) return "Usage: /search <keyword>";
        List<String> notes = taskManager.searchNotes(chatId, parts[1]);
        if (notes.isEmpty()) return "No notes found matching: " + parts[1];
        return "🔍 Search results for \"" + parts[1] + "\":\n\n" + String.join("\n\n", notes);
    }

    // ── Weather ──────────────────────────────────────────────────────────────

    private String getWeather(String chatId, String[] parts) {
        String location = parts.length > 1 ? parts[1] : userPrefs.getLocation(chatId);
        if (location == null) return "❌ No location set. Use: /setlocation <city>";
        return weatherService.getWeather(location);
    }

    private String setLocation(String chatId, String[] parts) {
        if (parts.length < 2) return "Usage: /setlocation <city>\nExample: /setlocation New York";
        String location = parts[1];
        userPrefs.setLocation(chatId, location);
        String weather = weatherService.getWeather(location);
        if (weather.startsWith("❌")) return weather;
        return "✅ Location set to: " + location + "\n\n" + weather;
    }

    private String toggleWeather(String chatId, boolean enabled) {
        userPrefs.setWeatherEnabled(chatId, enabled);
        if (enabled) {
            String location = userPrefs.getLocation(chatId);
            return "✅ Weather updates enabled" + (location != null ? " for " + location : "") + "!";
        }
        return "🔕 Weather updates disabled";
    }

    private String toggleHourly(String chatId, boolean enabled) {
        userPrefs.setHourlyWeather(chatId, enabled);
        return enabled ? "✅ Hourly weather updates enabled" : "🔕 Hourly weather updates disabled";
    }

    private String resetLocation(String chatId) {
        userPrefs.setLocation(chatId, "Atlanta");
        return "✅ Location reset to Atlanta";
    }

    // ── Stocks ───────────────────────────────────────────────────────────────

    private String addStock(String chatId, String[] parts) {
        if (parts.length < 2) return "Usage: /addstock <SYMBOL>";
        String symbol = parts[1].toUpperCase();
        taskManager.addStock(chatId, symbol);
        String quote = stockService.getStockQuote(symbol);
        return "✅ Added " + symbol + " to your watchlist!\n\n" + quote;
    }

    private String listStocks(String chatId) {
        List<String> stocks = taskManager.getStocks(chatId);
        if (stocks.isEmpty())
            return "📊 No stocks in your watchlist.\n\nAdd stocks with: /addstock <SYMBOL>";
        return "📊 Your Stock Watchlist:\n\n" + String.join(", ", stocks) +
                "\n\nHourly updates during NYSE hours (9:30 AM – 4:00 PM ET, Mon–Fri)";
    }

    private String deleteStock(String chatId, String[] parts) {
        if (parts.length < 2) return "Usage: /delstock <SYMBOL>";
        String symbol = parts[1].toUpperCase();
        return taskManager.deleteStock(chatId, symbol)
                ? "🗑️ Removed " + symbol + " from your watchlist"
                : "❌ " + symbol + " not found in your watchlist";
    }

    private String getStockUpdate(String chatId) {
        List<String> stocks = taskManager.getStocks(chatId);
        if (stocks.isEmpty()) return "📊 No stocks in watchlist. Add with /addstock <SYMBOL>";
        String report = stockService.getMultipleStocks(stocks);
        String status = stockService.isMarketOpen() ? "🟢 Market OPEN" : "🔴 Market CLOSED";
        return status + "\n\n" + report;
    }

    // ── Stock Alerts ─────────────────────────────────────────────────────────

    private String addAlert(String chatId, String[] parts) {
        if (parts.length < 2) return "Usage: /alert <SYMBOL> above/below <price>\nExample: /alert NVDA above 150";
        String[] tokens = parts[1].split("\\s+");
        if (tokens.length < 3) return "Usage: /alert <SYMBOL> above/below <price>";
        String symbol = tokens[0].toUpperCase();
        String direction = tokens[1].toLowerCase();
        if (!direction.equals("above") && !direction.equals("below"))
            return "❌ Direction must be 'above' or 'below'";
        try {
            double price = Double.parseDouble(tokens[2]);
            taskManager.addStockAlert(chatId, symbol, direction, price);
            return String.format("🔔 Alert set: notify when %s goes %s $%.2f", symbol, direction, price);
        } catch (NumberFormatException e) {
            return "❌ Invalid price: " + tokens[2];
        }
    }

    private String listAlerts(String chatId) {
        List<TaskManager.StockAlert> alerts = taskManager.getActiveAlerts(chatId);
        if (alerts.isEmpty()) return "🔔 No active alerts.\n\nSet one with: /alert NVDA above 150";
        StringBuilder sb = new StringBuilder("🔔 Active Stock Alerts:\n\n");
        for (TaskManager.StockAlert a : alerts) {
            sb.append(String.format("[%d] %s %s $%.2f\n", a.id, a.symbol, a.direction, a.targetPrice));
        }
        return sb.toString().trim();
    }

    private String deleteAlert(String chatId, String[] parts) {
        if (parts.length < 2) return "Usage: /delalert <id>";
        try {
            return taskManager.deleteAlert(chatId, Integer.parseInt(parts[1]))
                    ? "🗑️ Alert deleted!" : "❌ Alert not found";
        } catch (NumberFormatException e) {
            return "❌ Invalid ID";
        }
    }

    // ── Expenses ─────────────────────────────────────────────────────────────

    private String addExpense(String chatId, String[] parts) {
        if (parts.length < 2) return "Usage: /expense <amount> <description>\nExample: /expense 12.50 coffee";
        String[] tokens = parts[1].split("\\s+", 2);
        try {
            double amount = Double.parseDouble(tokens[0]);
            String description = tokens.length > 1 ? tokens[1] : "unspecified";
            taskManager.addExpense(chatId, amount, description);
            return String.format("💸 Expense logged: $%.2f — %s", amount, description);
        } catch (NumberFormatException e) {
            return "❌ Invalid amount. Usage: /expense 12.50 coffee";
        }
    }

    private String listExpenses(String chatId, String[] parts) {
        String yearMonth = parts.length > 1 ? parts[1] : YearMonth.now().toString();
        List<String> expenses = taskManager.getExpenses(chatId, yearMonth);
        double total = taskManager.getMonthlyTotal(chatId, yearMonth);
        if (expenses.isEmpty()) return "💸 No expenses found for " + yearMonth;
        return "💸 Expenses for " + yearMonth + ":\n\n" +
                String.join("\n", expenses) +
                String.format("\n\n💰 Total: $%.2f", total);
    }

    private String deleteExpense(String chatId, String[] parts) {
        if (parts.length < 2) return "Usage: /delexpense <id>";
        try {
            return taskManager.deleteExpense(chatId, Integer.parseInt(parts[1]))
                    ? "🗑️ Expense deleted!" : "❌ Expense not found";
        } catch (NumberFormatException e) {
            return "❌ Invalid ID";
        }
    }

    // ── Pomodoro ─────────────────────────────────────────────────────────────

    private String startPomodoro(String chatId, String[] parts) {
        int minutes = 25;
        if (parts.length > 1) {
            try {
                minutes = Integer.parseInt(parts[1]);
                if (minutes <= 0 || minutes > 180) return "❌ Duration must be between 1 and 180 minutes.";
            } catch (NumberFormatException e) {
                return "❌ Invalid duration. Usage: /pomodoro 25";
            }
        }
        LocalDateTime doneAt = LocalDateTime.now().plusMinutes(minutes);
        taskManager.addReminder(chatId, "🍅 Pomodoro done! Time for a break.", doneAt);
        return String.format("🍅 Pomodoro started! I'll remind you in %d minutes.\nFocus time: until %s",
                minutes, doneAt.format(displayFormatter));
    }

    // ── Digest ───────────────────────────────────────────────────────────────

    public String buildDigest(String chatId) {
        StringBuilder sb = new StringBuilder();
        sb.append("🌅 Good morning! Here's your daily digest:\n\n");

        // Weather
        String location = userPrefs.getLocation(chatId);
        if (location != null && userPrefs.isWeatherEnabled(chatId)) {
            sb.append("🌤️ Weather in ").append(location).append(":\n");
            String weather = weatherService.getWeather(location);
            // Include only first ~200 chars for brevity in the digest
            sb.append(weather.length() > 300 ? weather.substring(0, 300) + "…" : weather);
            sb.append("\n\n");
        }

        // Pending tasks
        List<String> tasks = taskManager.getTasks(chatId, false);
        sb.append("📋 Pending Tasks: ").append(tasks.size()).append("\n");
        if (!tasks.isEmpty()) {
            tasks.stream().limit(5).forEach(t -> sb.append(t).append("\n"));
            if (tasks.size() > 5) sb.append("… and ").append(tasks.size() - 5).append(" more\n");
        }
        sb.append("\n");

        // Today's reminders
        List<String> reminders = taskManager.getUpcomingReminders(chatId);
        sb.append("⏰ Upcoming Reminders: ").append(reminders.size()).append("\n");
        if (!reminders.isEmpty()) {
            reminders.stream().limit(5).forEach(r -> sb.append("• ").append(r).append("\n"));
        }
        sb.append("\n");

        // Stocks
        List<String> stocks = taskManager.getStocks(chatId);
        if (!stocks.isEmpty()) {
            boolean marketOpen = stockService.isMarketOpen();
            sb.append(marketOpen ? "📊 Market is OPEN:\n" : "📊 Latest Stock Prices:\n");
            sb.append(stockService.getMultipleStocks(stocks));
            sb.append("\n");
        }

        // Expenses this month
        double monthlyTotal = taskManager.getMonthlyTotal(chatId, null);
        if (monthlyTotal > 0) {
            sb.append(String.format("💸 Spending this month: $%.2f\n", monthlyTotal));
        }

        return sb.toString().trim();
    }

    // ── Help ─────────────────────────────────────────────────────────────────

    private String getHelpMessage() {
        return """
            🤖 Agent007 Commands:

            📋 Tasks:
            /addtask <task> — Add a task
            /tasks — Pending tasks
            /alltasks — All tasks
            /done <id> — Mark complete
            /deltask <id> — Delete task

            ⏰ Reminders:
            /remind in 30 minutes to call mom
            /remind every day at 8am to drink water
            /remind every monday at 9am to standup
            /reminders — List upcoming reminders
            /delreminder <id> — Delete reminder

            📝 Notes:
            /addnote <title> | <content> | <tags>
            /notes — All notes
            /notes <tag> — Filter by tag
            /search <keyword> — Search notes
            /delnote <id> — Delete note

            🌤️ Weather:
            /setlocation <city>
            /weather — Current weather
            /weatheron /weatheroff
            /hourlyon /hourlyoff

            📈 Stocks:
            /addstock <SYMBOL> — Add to watchlist
            /stocks — View watchlist
            /delstock <SYMBOL> — Remove stock
            /stocknow — Get current prices
            /alert NVDA above 150 — Set price alert
            /alerts — List alerts
            /delalert <id> — Delete alert

            💸 Expenses:
            /expense 12.50 coffee — Log expense
            /expenses — This month's spending
            /expenses 2026-03 — Specific month
            /delexpense <id> — Delete expense

            🍅 Pomodoro:
            /pomodoro — 25-min focus timer
            /pomodoro 50 — Custom duration

            🌅 Digest:
            /digest — Morning summary

            💬 Chat normally for AI conversation!
            💬 Say "remind me..." to set reminders!
            """;
    }
}
