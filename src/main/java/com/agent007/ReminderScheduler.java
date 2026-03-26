package com.agent007;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ReminderScheduler {
    private final TaskManager taskManager;
    private final Agent007Bot bot;
    private final ScheduledExecutorService scheduler;

    public ReminderScheduler(TaskManager taskManager, Agent007Bot bot) {
        this.taskManager = taskManager;
        this.bot = bot;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void start() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkAndSendReminders();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 30, TimeUnit.SECONDS);
    }

    private void checkAndSendReminders() {
        List<TaskManager.Reminder> reminders = taskManager.getPendingReminders();
        for (TaskManager.Reminder reminder : reminders) {
            bot.sendMessagePublic(reminder.chatId, "⏰ Reminder: " + reminder.message);
            taskManager.markReminderSent(reminder.id);
            System.out.println("[REMINDER SENT] Chat: " + reminder.chatId + " - " + reminder.message);
        }
    }

    public void stop() {
        scheduler.shutdown();
    }
}
