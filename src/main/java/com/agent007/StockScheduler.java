package com.agent007;

import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StockScheduler {
    private final StockService stockService;
    private final TaskManager taskManager;
    private final UserPreferences userPrefs;
    private final Agent007Bot bot;
    private final ScheduledExecutorService scheduler;
    private int lastHourSent = -1;

    public StockScheduler(StockService stockService, TaskManager taskManager,
                          UserPreferences userPrefs, Agent007Bot bot) {
        this.stockService = stockService;
        this.taskManager = taskManager;
        this.userPrefs = userPrefs;
        this.bot = bot;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void start() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkAndSendStockUpdates();
                checkPriceAlerts();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 5, TimeUnit.MINUTES);
    }

    private void checkAndSendStockUpdates() {
        if (!stockService.isMarketOpen()) return;

        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/New_York"));
        int currentHour = now.getHour();

        if (currentHour != lastHourSent && now.getMinute() < 5) {
            System.out.println("[STOCK SCHEDULER] Sending hourly stock updates...");
            String timeStr = now.format(DateTimeFormatter.ofPattern("h:mm a"));

            for (String chatId : userPrefs.getAllChatIdsWithWeatherEnabled()) {
                List<String> symbols = taskManager.getStocks(chatId);
                if (symbols.isEmpty()) continue;

                String stockReport = stockService.getMultipleStocks(symbols);
                bot.sendMessagePublic(chatId, String.format("""
                    📊 Hourly Stock Update — %s ET

                    %s
                    💼 Happy trading!
                    """, timeStr, stockReport));
            }
            lastHourSent = currentHour;
        }
    }

    private void checkPriceAlerts() {
        if (!stockService.isMarketOpen()) return;

        List<TaskManager.StockAlert> alerts = taskManager.getAllActiveAlerts();
        for (TaskManager.StockAlert alert : alerts) {
            double currentPrice = fetchPrice(alert.symbol);
            if (currentPrice <= 0) continue;

            boolean triggered = switch (alert.direction) {
                case "above" -> currentPrice >= alert.targetPrice;
                case "below" -> currentPrice <= alert.targetPrice;
                default -> false;
            };

            if (triggered) {
                taskManager.markAlertTriggered(alert.id);
                bot.sendMessagePublic(alert.chatId, String.format(
                        "🔔 Price Alert! %s is now $%.2f (%s your target of $%.2f)",
                        alert.symbol, currentPrice, alert.direction, alert.targetPrice));
                System.out.println("[ALERT FIRED] " + alert.symbol + " " + alert.direction + " " + alert.targetPrice);
            }
        }
    }

    private double fetchPrice(String symbol) {
        String quote = stockService.getStockQuote(symbol);
        // Quote format: "📈 NVDA: $150.23 +1.50 (+1.01%)"
        Pattern p = Pattern.compile("\\$([\\d.]+)");
        Matcher m = p.matcher(quote);
        if (m.find()) {
            try {
                return Double.parseDouble(m.group(1));
            } catch (NumberFormatException ignored) {}
        }
        return -1;
    }

    public void stop() {
        scheduler.shutdown();
    }
}
