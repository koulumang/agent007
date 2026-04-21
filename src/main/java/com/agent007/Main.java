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

            ReminderScheduler reminderScheduler = new ReminderScheduler(bot.getTaskManager(), bot);
            reminderScheduler.start();

            WeatherScheduler weatherScheduler = new WeatherScheduler(
                    bot.getWeatherService(), bot.getUserPrefs(), bot, bot.getOllama());
            weatherScheduler.start();

            StockScheduler stockScheduler = new StockScheduler(
                    bot.getStockService(), bot.getTaskManager(), bot.getUserPrefs(), bot);
            stockScheduler.start();

            DigestScheduler digestScheduler = new DigestScheduler(
                    bot.getCommandHandler(), bot.getUserPrefs(), bot);
            digestScheduler.start();

            System.out.println("Agent007 is online!");
            System.out.println("✅ Reminder scheduler — every 30s (with snooze + recurring support)");
            System.out.println("✅ Weather scheduler  — every 2h");
            System.out.println("✅ Stock scheduler    — hourly during NYSE hours + price alerts");
            System.out.println("✅ Digest scheduler   — daily at 8 AM");
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
