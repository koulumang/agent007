package com.agent007;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;

public class Agent007Bot extends TelegramLongPollingBot {
    private final String botToken;
    private final OllamaClient ollama;
    private final MemoryStore memory;
    private final TaskManager taskManager;
    private final CommandHandler commandHandler;
    private final WeatherService weatherService;
    private final UserPreferences userPrefs;
    private final StockService stockService;

    public Agent007Bot(String botToken) {
        this.botToken = botToken;
        this.ollama = new OllamaClient();
        this.memory = new MemoryStore();
        this.memory.setOllamaClient(ollama);
        this.taskManager = new TaskManager();
        this.weatherService = new WeatherService();
        this.userPrefs = new UserPreferences();
        this.stockService = new StockService();
        this.commandHandler = new CommandHandler(taskManager, weatherService, userPrefs, stockService);
    }

    @Override
    public String getBotUsername() {
        return "Agent007Bot";
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        // Handle inline keyboard callbacks (snooze buttons)
        if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
            return;
        }

        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        String chatId = update.getMessage().getChatId().toString();
        String userMessage = update.getMessage().getText();

        System.out.println("\n[RECEIVED] Chat ID: " + chatId);
        System.out.println("[USER] " + userMessage);

        // Initialize defaults for new users
        userPrefs.getLocation(chatId);
        taskManager.initializeDefaultStocks(chatId);

        // Commands
        if (userMessage.startsWith("/")) {
            String commandResponse = commandHandler.handleCommand(chatId, userMessage);
            if (commandResponse != null) {
                System.out.println("[REPLY] " + commandResponse);
                sendMessage(chatId, commandResponse);
                return;
            }
        }

        // Natural reminder in conversation
        if (NaturalReminderParser.isReminderRequest(userMessage)) {
            NaturalReminderParser.ParsedReminder parsed = NaturalReminderParser.parse(userMessage);
            if (parsed != null && !parsed.dateTime.isBefore(LocalDateTime.now())) {
                taskManager.addReminder(chatId, parsed.message, parsed.dateTime, parsed.recurrence);
                String recurrStr = parsed.recurrence != null ? " (repeats " + parsed.recurrence + ")" : "";
                String response = "⏰ Got it! Reminder set for " +
                        parsed.dateTime.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd 'at' h:mm a")) +
                        recurrStr + ": " + parsed.message;
                System.out.println("[REPLY] " + response);
                sendMessage(chatId, response);
                return;
            }
        }

        // AI conversation
        memory.saveMessage(chatId, "user", userMessage);
        String context = memory.buildContext(chatId);
        String response = ollama.chat(context, userMessage);
        System.out.println("[REPLY] " + response);
        memory.saveMessage(chatId, "assistant", response);
        memory.extractAndSaveFacts(chatId, userMessage, response);
        sendMessage(chatId, response);
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        String chatId = callbackQuery.getMessage().getChatId().toString();

        if (data != null && data.startsWith("snooze_")) {
            String[] parts = data.split("_");
            if (parts.length >= 3) {
                try {
                    int reminderId = Integer.parseInt(parts[1]);
                    int minutes = Integer.parseInt(parts[2]);
                    String originalMessage = taskManager.getReminderMessage(reminderId);
                    if (originalMessage != null) {
                        LocalDateTime snoozeTime = LocalDateTime.now().plusMinutes(minutes);
                        String label = minutes >= 60 ? (minutes / 60) + " hour" : minutes + " min";
                        taskManager.addReminder(chatId, originalMessage, snoozeTime);
                        sendMessage(chatId, "⏱️ Snoozed for " + label + "!");
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        // Acknowledge the callback so the button stops loading
        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(callbackQuery.getId());
        try {
            execute(answer);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendMessagePublic(String chatId, String text) {
        sendMessage(chatId, text);
    }

    public void sendReminderWithSnooze(String chatId, String text, InlineKeyboardMarkup keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setReplyMarkup(keyboard);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public TaskManager getTaskManager() { return taskManager; }
    public WeatherService getWeatherService() { return weatherService; }
    public UserPreferences getUserPrefs() { return userPrefs; }
    public OllamaClient getOllama() { return ollama; }
    public StockService getStockService() { return stockService; }
    public CommandHandler getCommandHandler() { return commandHandler; }
}
