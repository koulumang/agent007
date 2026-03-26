package com.agent007;

import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StockScheduler {
    private final StockService stockService;
    private final TaskManager taskManager;
    private final UserPreferences userPrefs;
    private final Agent007Bot bot;
    private final ScheduledExecutorService scheduler;
    private int lastHourSent = -1;

    public StockScheduler(StockService stockService, TaskManager taskManager, UserPreferences userPrefs, Agent007Bot bot) {
        this.stockService = stockService;
        this.taskManager = taskManager;
        this.userPrefs = userPrefs;
        this.bot = bot;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void start() {
        // Check every 5 minutes during market hours
        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkAndSendStockUpdates();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 5, TimeUnit.MINUTES);
    }

    private void checkAndSendStockUpdates() {
        if (!stockService.isMarketOpen()) {
            return; // Market is closed
        }

        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/New_York"));
        int currentHour = now.getHour();

        // Send update on the hour (if we haven't sent this hour yet)
        if (currentHour != lastHourSent && now.getMinute() < 5) {
            System.out.println("[STOCK SCHEDULER] Sending hourly stock updates...");
            
            List<String> chatIds = userPrefs.getAllChatIdsWithWeatherEnabled(); // Reuse this for active users
            
            for (String chatId : chatIds) {
                List<String> symbols = taskManager.getStocks(chatId);
                
                if (symbols.isEmpty()) {
                    continue;
                }
                
                String stockReport = stockService.getMultipleStocks(symbols);
                String message = String.format("""
                    📊 Hourly Stock Market Update
                    %s ET
                    
                    %s
                    
                    💼 Happy trading!
                    """, now.format(java.time.format.DateTimeFormatter.ofPattern("h:mm a")), stockReport);
                
                bot.sendMessagePublic(chatId, message);
                System.out.println("[STOCK] Sent hourly update to " + chatId);
            }
            
            lastHourSent = currentHour;
        }
    }

    public void stop() {
        scheduler.shutdown();
    }
}
