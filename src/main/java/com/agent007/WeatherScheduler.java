package com.agent007;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WeatherScheduler {
    private final WeatherService weatherService;
    private final UserPreferences userPrefs;
    private final Agent007Bot bot;
    private final OllamaClient ollama;
    private final ScheduledExecutorService scheduler;
    private LocalDateTime lastUpdateTime = null;

    public WeatherScheduler(WeatherService weatherService, UserPreferences userPrefs, Agent007Bot bot, OllamaClient ollama) {
        this.weatherService = weatherService;
        this.userPrefs = userPrefs;
        this.bot = bot;
        this.ollama = ollama;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void start() {
        // Check every 10 minutes, send updates every 2 hours
        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkAndSendWeatherUpdates();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 10, TimeUnit.MINUTES);
    }

    private void checkAndSendWeatherUpdates() {
        LocalDateTime now = LocalDateTime.now();

        // Check if 2 hours have passed since last update
        if (lastUpdateTime != null && now.isBefore(lastUpdateTime.plusHours(2))) {
            return; // Not time yet
        }

        System.out.println("[WEATHER SCHEDULER] Time for 2-hour weather update!");
        
        List<String> chatIds = userPrefs.getAllChatIdsWithWeatherEnabled();
        System.out.println("[WEATHER SCHEDULER] Found " + chatIds.size() + " users with weather enabled");

        for (String chatId : chatIds) {
            String location = userPrefs.getLocation(chatId);
            System.out.println("[WEATHER SCHEDULER] Chat " + chatId + " location: " + location);
            if (location == null) continue;

            System.out.println("[WEATHER SCHEDULER] Fetching weather for " + location);
            String weather = weatherService.getWeather(location);
            String tips = generateWeatherTips(weather);
            
            String message = String.format("""                
                %s
                
                💡 Weather Tips:
                %s
                
                Stay safe and have a great day! 🌟
                """, weather, tips);
            
            bot.sendMessagePublic(chatId, message);
            System.out.println("[WEATHER] Sent 2-hour weather update to " + chatId);
        }
        
        lastUpdateTime = now;
    }

    private String generateWeatherTips(String weather) {
        String prompt = String.format("""
            Based on this weather report, generate 3-4 witty, practical, and helpful tips for someone going about their day.
            Be conversational, friendly, and add a touch of humor where appropriate.
            Keep each tip concise (one line). Use bullet points.
            
            Weather Report:
            %s
            
            Generate weather tips:
            """, weather);
        
        try {
            String tips = ollama.chat("", prompt);
            // Clean up the response
            tips = tips.trim();
            if (!tips.startsWith("•") && !tips.startsWith("-")) {
                // If LLM didn't use bullet points, add them
                String[] lines = tips.split("\n");
                StringBuilder formatted = new StringBuilder();
                for (String line : lines) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("•") && !line.startsWith("-")) {
                        formatted.append("• ").append(line).append("\n");
                    } else if (!line.isEmpty()) {
                        formatted.append(line).append("\n");
                    }
                }
                tips = formatted.toString().trim();
            }
            return tips;
        } catch (Exception e) {
            e.printStackTrace();
            return "• Check the weather before heading out\n• Dress appropriately\n• Have a wonderful day!";
        }
    }

    public void stop() {
        scheduler.shutdown();
    }
}
