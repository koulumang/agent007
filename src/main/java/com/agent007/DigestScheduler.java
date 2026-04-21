package com.agent007;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DigestScheduler {
    private final CommandHandler commandHandler;
    private final UserPreferences userPrefs;
    private final Agent007Bot bot;
    private final ScheduledExecutorService scheduler;
    private LocalDate lastDigestDate = null;

    public DigestScheduler(CommandHandler commandHandler, UserPreferences userPrefs, Agent007Bot bot) {
        this.commandHandler = commandHandler;
        this.userPrefs = userPrefs;
        this.bot = bot;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void start() {
        // Check every minute; send digest at 8 AM once per day
        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkAndSendDigest();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    private void checkAndSendDigest() {
        LocalTime now = LocalTime.now();
        LocalDate today = LocalDate.now();

        boolean isDigestTime = now.getHour() == 8 && now.getMinute() < 1;
        boolean notSentToday = !today.equals(lastDigestDate);

        if (isDigestTime && notSentToday) {
            List<String> chatIds = userPrefs.getAllActiveChatIds();
            System.out.println("[DIGEST] Sending morning digest to " + chatIds.size() + " users...");
            for (String chatId : chatIds) {
                String digest = commandHandler.buildDigest(chatId);
                bot.sendMessagePublic(chatId, digest);
            }
            lastDigestDate = today;
        }
    }

    public void stop() {
        scheduler.shutdown();
    }
}
