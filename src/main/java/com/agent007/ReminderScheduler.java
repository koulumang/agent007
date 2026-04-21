package com.agent007;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
            sendReminderWithSnooze(reminder);
            taskManager.markReminderSent(reminder.id);
            System.out.println("[REMINDER SENT] Chat: " + reminder.chatId + " - " + reminder.message);

            // Re-schedule if recurring
            if (reminder.recurrence != null) {
                LocalDateTime next = nextOccurrence(reminder.recurrence);
                if (next != null) {
                    taskManager.addReminder(reminder.chatId, reminder.message, next, reminder.recurrence);
                    System.out.println("[RECURRING] Next " + reminder.recurrence + " reminder at " + next);
                }
            }
        }
    }

    private void sendReminderWithSnooze(TaskManager.Reminder reminder) {
        InlineKeyboardMarkup keyboard = buildSnoozeKeyboard(reminder.id);
        bot.sendReminderWithSnooze(reminder.chatId, "⏰ Reminder: " + reminder.message, keyboard);
    }

    private InlineKeyboardMarkup buildSnoozeKeyboard(int reminderId) {
        InlineKeyboardButton snooze15 = new InlineKeyboardButton();
        snooze15.setText("Snooze 15 min");
        snooze15.setCallbackData("snooze_" + reminderId + "_15");

        InlineKeyboardButton snooze60 = new InlineKeyboardButton();
        snooze60.setText("Snooze 1 hour");
        snooze60.setCallbackData("snooze_" + reminderId + "_60");

        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(snooze15);
        row.add(snooze60);

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(row);

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        keyboard.setKeyboard(rows);
        return keyboard;
    }

    private LocalDateTime nextOccurrence(String recurrence) {
        return switch (recurrence.toLowerCase()) {
            case "daily" -> LocalDateTime.now().plusDays(1);
            case "weekly" -> LocalDateTime.now().plusWeeks(1);
            case "monthly" -> LocalDateTime.now().plusMonths(1);
            default -> null;
        };
    }

    public void stop() {
        scheduler.shutdown();
    }
}
