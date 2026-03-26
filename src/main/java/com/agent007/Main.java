package com.agent007;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
    public static void main(String[] args) {
        String botToken = System.getenv("TELEGRAM_BOT_TOKEN");
        if (botToken == null || botToken.isEmpty()) {
            System.err.println("TELEGRAM_BOT_TOKEN environment variable not set");
            System.exit(1);
        }

        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            Agent007Bot bot = new Agent007Bot(botToken);
            botsApi.registerBot(bot);
            
            // Start reminder scheduler
            ReminderScheduler reminderScheduler = new ReminderScheduler(bot.getTaskManager(), bot);
            reminderScheduler.start();
            
            // Start weather scheduler
            WeatherScheduler weatherScheduler = new WeatherScheduler(
                bot.getWeatherService(), 
                bot.getUserPrefs(), 
                bot,
                bot.getOllama()
            );
            weatherScheduler.start();
            
            // Start stock scheduler
            StockScheduler stockScheduler = new StockScheduler(
                bot.getStockService(),
                bot.getTaskManager(),
                bot.getUserPrefs(),
                bot
            );
            stockScheduler.start();
            
            System.out.println("Agent007 is online!");
            System.out.println("✅ Reminder scheduler started - checking every 30 seconds");
            System.out.println("✅ Weather scheduler started - updates every 2 hours");
            System.out.println("✅ Stock scheduler started - hourly updates during NYSE hours (9:30 AM - 4:00 PM ET, Mon-Fri)");
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
