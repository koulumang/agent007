package com.agent007;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class CommandHandler {
    private final TaskManager taskManager;
    private final WeatherService weatherService;
    private final UserPreferences userPrefs;
    private final StockService stockService;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public CommandHandler(TaskManager taskManager, WeatherService weatherService, UserPreferences userPrefs, StockService stockService) {
        this.taskManager = taskManager;
        this.weatherService = weatherService;
        this.userPrefs = userPrefs;
        this.stockService = stockService;
    }

    public String handleCommand(String chatId, String message) {
        String[] parts = message.split(" ", 2);
        String command = parts[0].toLowerCase();

        switch (command) {
            case "/help":
                return getHelpMessage();
            
            case "/addtask":
                if (parts.length < 2) return "Usage: /addtask <task description>";
                taskManager.addTask(chatId, parts[1]);
                return "✅ Task added!";
            
            case "/tasks":
                return getTasksList(chatId, false);
            
            case "/alltasks":
                return getTasksList(chatId, true);
            
            case "/done":
                return completeTask(chatId, parts);
            
            case "/deltask":
                return deleteTask(chatId, parts);
            
            case "/remind":
                return addReminder(chatId, parts);
            
            case "/addnote":
                return addNote(chatId, parts);
            
            case "/notes":
                return getNotes(chatId, parts);
            
            case "/delnote":
                return deleteNote(chatId, parts);
            
            case "/weather":
                return getWeather(chatId, parts);
            
            case "/setlocation":
                return setLocation(chatId, parts);
            
            case "/weatheron":
                return toggleWeather(chatId, true);
            
            case "/weatheroff":
                return toggleWeather(chatId, false);
            
            case "/hourlyon":
                return toggleHourly(chatId, true);
            
            case "/hourlyoff":
                return toggleHourly(chatId, false);
            
            case "/resetlocation":
                userPrefs.setLocation(chatId, "Atlanta");
                return "✅ Location reset to Atlanta";
            
            case "/addstock":
                return addStock(chatId, parts);
            
            case "/stocks":
                return listStocks(chatId);
            
            case "/delstock":
                return deleteStock(chatId, parts);
            
            case "/stocknow":
                return getStockUpdate(chatId);
            
            default:
                return null; // Not a command
        }
    }

    private String getHelpMessage() {
        return """
            🤖 Agent007 Commands:
            
            📋 Tasks:
            /addtask <task> - Add a new task
            /tasks - Show pending tasks
            /alltasks - Show all tasks
            /done <id> - Mark task as complete
            /deltask <id> - Delete a task
            
            ⏰ Reminders (Natural Language):
            /remind in 30 minutes to call mom
            /remind tomorrow at 3pm about meeting
            /remind at 5pm to go to gym
            /remind on monday at 9am about standup
            /remind next week to review code
            
            📝 Notes:
            /addnote <title> | <content> | <tags>
            Example: /addnote Meeting | Discuss Q2 goals | work,important
            /notes - Show all notes
            /notes <tag> - Show notes with tag
            /delnote <id> - Delete a note
            
            🌤️ Weather (Auto-enabled):
            /setlocation <city> - Set your location
            /weather - Check current weather
            /weatheroff - Disable auto weather
            /weatheron - Enable auto weather
            /hourlyoff - Disable hourly updates
            /hourlyon - Enable hourly updates
            
            📈 Stock Market (NYSE Hours):
            /addstock <SYMBOL> - Add stock to track
            /stocks - List your tracked stocks
            /delstock <SYMBOL> - Remove a stock
            /stocknow - Get current stock prices
            
            💬 Just say "remind me..." in conversation and I'll understand!
            💬 Chat normally for AI conversation!
            """;
    }

    private String getTasksList(String chatId, boolean includeCompleted) {
        List<String> tasks = taskManager.getTasks(chatId, includeCompleted);
        if (tasks.isEmpty()) {
            return "No tasks found!";
        }
        return "📋 Your Tasks:\n\n" + String.join("\n", tasks);
    }

    private String completeTask(String chatId, String[] parts) {
        if (parts.length < 2) return "Usage: /done <task_id>";
        try {
            int taskId = Integer.parseInt(parts[1]);
            if (taskManager.completeTask(chatId, taskId)) {
                return "✅ Task completed!";
            }
            return "❌ Task not found";
        } catch (NumberFormatException e) {
            return "❌ Invalid task ID";
        }
    }

    private String deleteTask(String chatId, String[] parts) {
        if (parts.length < 2) return "Usage: /deltask <task_id>";
        try {
            int taskId = Integer.parseInt(parts[1]);
            if (taskManager.deleteTask(chatId, taskId)) {
                return "🗑️ Task deleted!";
            }
            return "❌ Task not found";
        } catch (NumberFormatException e) {
            return "❌ Invalid task ID";
        }
    }

    private String addReminder(String chatId, String[] parts) {
        if (parts.length < 2) {
            return """
                ⏰ Reminder Examples:
                • remind me in 30 minutes to call mom
                • remind me in 2 hours about the meeting
                • remind me tomorrow at 3pm to submit report
                • remind me tomorrow about dentist appointment
                • remind me at 5pm to go to gym
                • remind me on monday at 9am about team meeting
                • remind me next week to review code
                
                Or use: /remind <YYYY-MM-DD HH:MM> <message>
                """;
        }
        
        String fullMessage = parts[1];
        
        // Try natural language parsing first
        NaturalReminderParser.ParsedReminder parsed = NaturalReminderParser.parse("remind me " + fullMessage);
        if (parsed != null) {
            if (parsed.dateTime.isBefore(LocalDateTime.now())) {
                return "❌ Cannot set reminder in the past!";
            }
            taskManager.addReminder(chatId, parsed.message, parsed.dateTime);
            DateTimeFormatter displayFormat = DateTimeFormatter.ofPattern("MMM dd 'at' h:mm a");
            return "⏰ Reminder set for " + parsed.dateTime.format(displayFormat) + "\n📝 " + parsed.message;
        }
        
        // Fall back to strict format: YYYY-MM-DD HH:MM message
        String[] reminderParts = fullMessage.split(" ", 3);
        if (reminderParts.length < 3) {
            return "❌ I couldn't understand that. Try: 'in 30 minutes to call mom' or use format: YYYY-MM-DD HH:MM message";
        }
        
        try {
            String dateTimeStr = reminderParts[0] + " " + reminderParts[1];
            LocalDateTime remindAt = LocalDateTime.parse(dateTimeStr, formatter);
            String message = reminderParts[2];
            
            if (remindAt.isBefore(LocalDateTime.now())) {
                return "❌ Cannot set reminder in the past!";
            }
            
            taskManager.addReminder(chatId, message, remindAt);
            return "⏰ Reminder set for " + dateTimeStr;
        } catch (DateTimeParseException e) {
            return "❌ I couldn't understand that. Try: 'in 30 minutes to call mom' or use format: YYYY-MM-DD HH:MM message";
        }
    }

    private String addNote(String chatId, String[] parts) {
        if (parts.length < 2) {
            return "Usage: /addnote <title> | <content> | <tags>\nExample: /addnote Meeting | Discuss goals | work";
        }
        
        String[] noteParts = parts[1].split("\\|");
        if (noteParts.length < 2) {
            return "❌ Invalid format. Use: /addnote <title> | <content> | <tags>";
        }
        
        String title = noteParts[0].trim();
        String content = noteParts[1].trim();
        String tags = noteParts.length > 2 ? noteParts[2].trim() : "";
        
        taskManager.addNote(chatId, title, content, tags);
        return "📝 Note saved!";
    }

    private String getNotes(String chatId, String[] parts) {
        String tag = parts.length > 1 ? parts[1] : null;
        List<String> notes = taskManager.getNotes(chatId, tag);
        
        if (notes.isEmpty()) {
            return tag != null ? "No notes found with tag: " + tag : "No notes found!";
        }
        
        return "📝 Your Notes:\n\n" + String.join("\n\n", notes);
    }

    private String deleteNote(String chatId, String[] parts) {
        if (parts.length < 2) return "Usage: /delnote <note_id>";
        try {
            int noteId = Integer.parseInt(parts[1]);
            if (taskManager.deleteNote(chatId, noteId)) {
                return "🗑️ Note deleted!";
            }
            return "❌ Note not found";
        } catch (NumberFormatException e) {
            return "❌ Invalid note ID";
        }
    }

    private String getWeather(String chatId, String[] parts) {
        String location;
        if (parts.length > 1) {
            location = parts[1];
        } else {
            location = userPrefs.getLocation(chatId);
            if (location == null) {
                return "❌ No location set. Use: /setlocation <city>";
            }
        }
        return weatherService.getWeather(location);
    }

    private String setLocation(String chatId, String[] parts) {
        if (parts.length < 2) {
            return "Usage: /setlocation <city>\nExample: /setlocation New York";
        }
        String location = parts[1];
        userPrefs.setLocation(chatId, location);
        
        // Test the location
        String weather = weatherService.getWeather(location);
        if (weather.startsWith("❌")) {
            return weather;
        }
        
        return "✅ Location set to: " + location + "\n\n" + 
               "🌤️ Weather updates enabled!\n" +
               "• Hourly updates every hour\n" +
               "• Daily summary at 8 AM\n\n" +
               "Current weather:\n" + weather;
    }

    private String toggleWeather(String chatId, boolean enabled) {
        userPrefs.setWeatherEnabled(chatId, enabled);
        if (enabled) {
            String location = userPrefs.getLocation(chatId);
            if (location == null) {
                return "✅ Weather enabled! Set your location with: /setlocation <city>";
            }
            return "✅ Weather updates enabled for " + location;
        } else {
            return "🔕 Weather updates disabled";
        }
    }

    private String toggleHourly(String chatId, boolean enabled) {
        userPrefs.setHourlyWeather(chatId, enabled);
        return enabled ? "✅ Hourly weather updates enabled" : "🔕 Hourly weather updates disabled (daily summary still active)";
    }

    private String addStock(String chatId, String[] parts) {
        if (parts.length < 2) {
            return "Usage: /addstock <SYMBOL>\nExample: /addstock AAPL";
        }
        String symbol = parts[1].toUpperCase();
        taskManager.addStock(chatId, symbol);
        
        // Test the symbol
        String quote = stockService.getStockQuote(symbol);
        if (quote.startsWith("❌")) {
            return quote + "\n\nStock symbol may be invalid, but it's been added to your list.";
        }
        
        return "✅ Added " + symbol + " to your watchlist!\n\n" + quote;
    }

    private String listStocks(String chatId) {
        List<String> stocks = taskManager.getStocks(chatId);
        if (stocks.isEmpty()) {
            return "📊 No stocks in your watchlist.\n\nAdd stocks with: /addstock <SYMBOL>\nDefault stocks: RBLX, TEAM, AMZN, NVDA";
        }
        return "📊 Your Stock Watchlist:\n\n" + String.join(", ", stocks) + 
               "\n\nYou'll receive hourly updates during NYSE trading hours (9:30 AM - 4:00 PM ET, Mon-Fri)";
    }

    private String deleteStock(String chatId, String[] parts) {
        if (parts.length < 2) {
            return "Usage: /delstock <SYMBOL>\nExample: /delstock AAPL";
        }
        String symbol = parts[1].toUpperCase();
        if (taskManager.deleteStock(chatId, symbol)) {
            return "🗑️ Removed " + symbol + " from your watchlist";
        }
        return "❌ " + symbol + " not found in your watchlist";
    }

    private String getStockUpdate(String chatId) {
        List<String> stocks = taskManager.getStocks(chatId);
        if (stocks.isEmpty()) {
            return "📊 No stocks in your watchlist. Add some with /addstock <SYMBOL>";
        }
        
        String report = stockService.getMultipleStocks(stocks);
        boolean marketOpen = stockService.isMarketOpen();
        String status = marketOpen ? "🟢 Market is OPEN" : "🔴 Market is CLOSED";
        
        return status + "\n\n" + report;
    }
}

