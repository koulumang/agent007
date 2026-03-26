package com.agent007;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
        if (update.hasMessage() && update.getMessage().hasText()) {
            String chatId = update.getMessage().getChatId().toString();
            String userMessage = update.getMessage().getText();

            System.out.println("\n[RECEIVED] Chat ID: " + chatId);
            System.out.println("[USER] " + userMessage);

            // Initialize default location for new users
            String currentLocation = userPrefs.getLocation(chatId);
            if (currentLocation == null || currentLocation.equals("Atlanta")) {
                userPrefs.setLocation(chatId, "Atlanta");
                System.out.println("[INIT] Ensured default location for chat " + chatId);
            }

            // Initialize default stocks for new users
            taskManager.initializeDefaultStocks(chatId);

            // Check if it's a command
            if (userMessage.startsWith("/")) {
                String commandResponse = commandHandler.handleCommand(chatId, userMessage);
                if (commandResponse != null) {
                    System.out.println("[REPLY] " + commandResponse);
                    System.out.println("---");
                    sendMessage(chatId, commandResponse);
                    return;
                }
            }

            // Check for natural reminder in conversation
            if (NaturalReminderParser.isReminderRequest(userMessage)) {
                NaturalReminderParser.ParsedReminder parsed = NaturalReminderParser.parse(userMessage);
                if (parsed != null && !parsed.dateTime.isBefore(LocalDateTime.now())) {
                    taskManager.addReminder(chatId, parsed.message, parsed.dateTime);
                    java.time.format.DateTimeFormatter displayFormat = 
                        java.time.format.DateTimeFormatter.ofPattern("MMM dd 'at' h:mm a");
                    String response = "⏰ Got it! I'll remind you on " + 
                        parsed.dateTime.format(displayFormat) + " about: " + parsed.message;
                    System.out.println("[REPLY] " + response);
                    System.out.println("---");
                    sendMessage(chatId, response);
                    return;
                }
            }

            // Regular AI conversation
            memory.saveMessage(chatId, "user", userMessage);
            
            String context = memory.buildContext(chatId);
            String response = ollama.chat(context, userMessage);
            
            System.out.println("[REPLY] " + response);
            System.out.println("---");
            
            memory.saveMessage(chatId, "assistant", response);
            memory.extractAndSaveFacts(chatId, userMessage, response);

            sendMessage(chatId, response);
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

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public WeatherService getWeatherService() {
        return weatherService;
    }

    public UserPreferences getUserPrefs() {
        return userPrefs;
    }

    public OllamaClient getOllama() {
        return ollama;
    }

    public StockService getStockService() {
        return stockService;
    }
}
