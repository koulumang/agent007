package com.agent007;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NaturalReminderParser {

    public static class ParsedReminder {
        public final LocalDateTime dateTime;
        public final String message;
        public final String recurrence; // null, "daily", "weekly"

        public ParsedReminder(LocalDateTime dateTime, String message) {
            this(dateTime, message, null);
        }

        public ParsedReminder(LocalDateTime dateTime, String message, String recurrence) {
            this.dateTime = dateTime;
            this.message = message;
            this.recurrence = recurrence;
        }
    }

    public static ParsedReminder parse(String text) {
        text = text.toLowerCase().trim();

        // ── Recurring: "remind me every day at Xpm to ..." ───────────────────
        Pattern everyDayPattern = Pattern.compile(
                "remind me every day\\s*(?:at)?\\s*(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\\s*(?:to|about)?\\s*(.+)",
                Pattern.CASE_INSENSITIVE);
        Matcher everyDayMatcher = everyDayPattern.matcher(text);
        if (everyDayMatcher.find()) {
            int hour = Integer.parseInt(everyDayMatcher.group(1));
            int minute = everyDayMatcher.group(2) != null ? Integer.parseInt(everyDayMatcher.group(2)) : 0;
            hour = applyAmPm(hour, everyDayMatcher.group(3));
            String message = everyDayMatcher.group(4);
            LocalDateTime remindAt = LocalDateTime.now().withHour(hour).withMinute(minute).withSecond(0);
            if (remindAt.isBefore(LocalDateTime.now())) remindAt = remindAt.plusDays(1);
            return new ParsedReminder(remindAt, message, "daily");
        }

        // ── Recurring: "remind me every monday at Xpm to ..." ────────────────
        Pattern everyDowPattern = Pattern.compile(
                "remind me every (monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\s*(?:at)?\\s*(\\d{1,2})?(?::(\\d{2}))?\\s*(am|pm)?\\s*(?:to|about)?\\s*(.+)",
                Pattern.CASE_INSENSITIVE);
        Matcher everyDowMatcher = everyDowPattern.matcher(text);
        if (everyDowMatcher.find()) {
            String dayName = everyDowMatcher.group(1).toLowerCase();
            String hourStr = everyDowMatcher.group(2);
            String minuteStr = everyDowMatcher.group(3);
            int hour = hourStr != null ? Integer.parseInt(hourStr) : 9;
            int minute = minuteStr != null ? Integer.parseInt(minuteStr) : 0;
            hour = applyAmPm(hour, everyDowMatcher.group(4));
            String message = everyDowMatcher.group(5);
            int targetDay = getDayOfWeekNumber(dayName);
            int currentDay = LocalDateTime.now().getDayOfWeek().getValue();
            int daysToAdd = (targetDay - currentDay + 7) % 7;
            if (daysToAdd == 0) daysToAdd = 7;
            LocalDateTime remindAt = LocalDateTime.now().plusDays(daysToAdd)
                    .withHour(hour).withMinute(minute).withSecond(0);
            return new ParsedReminder(remindAt, message, "weekly");
        }

        // ── Recurring: "remind me every week to ..." ─────────────────────────
        Pattern everyWeekPattern = Pattern.compile(
                "remind me every week\\s*(?:to|about)?\\s*(.+)",
                Pattern.CASE_INSENSITIVE);
        Matcher everyWeekMatcher = everyWeekPattern.matcher(text);
        if (everyWeekMatcher.find()) {
            LocalDateTime remindAt = LocalDateTime.now().plusWeeks(1)
                    .withHour(9).withMinute(0).withSecond(0);
            return new ParsedReminder(remindAt, everyWeekMatcher.group(1), "weekly");
        }

        // ── One-shot: "remind me in X minutes/hours/days to ..." ─────────────
        Pattern inPattern = Pattern.compile(
                "remind me in (\\d+)\\s*(minute|minutes|min|hour|hours|hr|day|days)\\s*(?:to|about)?\\s*(.+)",
                Pattern.CASE_INSENSITIVE);
        Matcher inMatcher = inPattern.matcher(text);
        if (inMatcher.find()) {
            int amount = Integer.parseInt(inMatcher.group(1));
            String unit = inMatcher.group(2).toLowerCase();
            String message = inMatcher.group(3);
            LocalDateTime remindAt = LocalDateTime.now();
            if (unit.startsWith("min")) remindAt = remindAt.plusMinutes(amount);
            else if (unit.startsWith("hour") || unit.equals("hr")) remindAt = remindAt.plusHours(amount);
            else if (unit.startsWith("day")) remindAt = remindAt.plusDays(amount);
            return new ParsedReminder(remindAt, message);
        }

        // ── One-shot: "remind me tomorrow at Xpm to ..." ─────────────────────
        Pattern tomorrowPattern = Pattern.compile(
                "remind me tomorrow\\s*(?:at)?\\s*(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\\s*(?:to|about)?\\s*(.+)",
                Pattern.CASE_INSENSITIVE);
        Matcher tomorrowMatcher = tomorrowPattern.matcher(text);
        if (tomorrowMatcher.find()) {
            int hour = Integer.parseInt(tomorrowMatcher.group(1));
            int minute = tomorrowMatcher.group(2) != null ? Integer.parseInt(tomorrowMatcher.group(2)) : 0;
            hour = applyAmPm(hour, tomorrowMatcher.group(3));
            LocalDateTime remindAt = LocalDateTime.now().plusDays(1)
                    .withHour(hour).withMinute(minute).withSecond(0);
            return new ParsedReminder(remindAt, tomorrowMatcher.group(4));
        }

        // ── One-shot: "remind me tomorrow to ..." (default 9 AM) ─────────────
        Pattern tomorrowSimplePattern = Pattern.compile(
                "remind me tomorrow\\s*(?:to|about)?\\s*(.+)",
                Pattern.CASE_INSENSITIVE);
        Matcher tomorrowSimpleMatcher = tomorrowSimplePattern.matcher(text);
        if (tomorrowSimpleMatcher.find()) {
            LocalDateTime remindAt = LocalDateTime.now().plusDays(1)
                    .withHour(9).withMinute(0).withSecond(0);
            return new ParsedReminder(remindAt, tomorrowSimpleMatcher.group(1));
        }

        // ── One-shot: "remind me at Xpm to ..." ──────────────────────────────
        Pattern atTimePattern = Pattern.compile(
                "remind me\\s*(?:at)?\\s*(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\\s*(?:to|about)?\\s*(.+)",
                Pattern.CASE_INSENSITIVE);
        Matcher atTimeMatcher = atTimePattern.matcher(text);
        if (atTimeMatcher.find()) {
            int hour = Integer.parseInt(atTimeMatcher.group(1));
            int minute = atTimeMatcher.group(2) != null ? Integer.parseInt(atTimeMatcher.group(2)) : 0;
            hour = applyAmPm(hour, atTimeMatcher.group(3));
            LocalDateTime remindAt = LocalDateTime.now().withHour(hour).withMinute(minute).withSecond(0);
            if (remindAt.isBefore(LocalDateTime.now())) remindAt = remindAt.plusDays(1);
            return new ParsedReminder(remindAt, atTimeMatcher.group(4));
        }

        // ── One-shot: "remind me next week to ..." ───────────────────────────
        Pattern nextWeekPattern = Pattern.compile(
                "remind me next week\\s*(?:to|about)?\\s*(.+)",
                Pattern.CASE_INSENSITIVE);
        Matcher nextWeekMatcher = nextWeekPattern.matcher(text);
        if (nextWeekMatcher.find()) {
            LocalDateTime remindAt = LocalDateTime.now().plusWeeks(1)
                    .withHour(9).withMinute(0).withSecond(0);
            return new ParsedReminder(remindAt, nextWeekMatcher.group(1));
        }

        // ── One-shot: "remind me on monday at Xpm to ..." ────────────────────
        Pattern dayOfWeekPattern = Pattern.compile(
                "remind me (?:on\\s+)?(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\s*(?:at)?\\s*(\\d{1,2})?(?::(\\d{2}))?\\s*(am|pm)?\\s*(?:to|about)?\\s*(.+)",
                Pattern.CASE_INSENSITIVE);
        Matcher dayOfWeekMatcher = dayOfWeekPattern.matcher(text);
        if (dayOfWeekMatcher.find()) {
            String dayName = dayOfWeekMatcher.group(1).toLowerCase();
            String hourStr = dayOfWeekMatcher.group(2);
            String minuteStr = dayOfWeekMatcher.group(3);
            int hour = hourStr != null ? Integer.parseInt(hourStr) : 9;
            int minute = minuteStr != null ? Integer.parseInt(minuteStr) : 0;
            hour = applyAmPm(hour, dayOfWeekMatcher.group(4));
            int targetDay = getDayOfWeekNumber(dayName);
            int currentDay = LocalDateTime.now().getDayOfWeek().getValue();
            int daysToAdd = (targetDay - currentDay + 7) % 7;
            if (daysToAdd == 0) daysToAdd = 7;
            LocalDateTime remindAt = LocalDateTime.now().plusDays(daysToAdd)
                    .withHour(hour).withMinute(minute).withSecond(0);
            return new ParsedReminder(remindAt, dayOfWeekMatcher.group(5));
        }

        return null;
    }

    private static int applyAmPm(int hour, String ampm) {
        if (ampm == null) return hour;
        if (ampm.equalsIgnoreCase("pm") && hour < 12) return hour + 12;
        if (ampm.equalsIgnoreCase("am") && hour == 12) return 0;
        return hour;
    }

    private static int getDayOfWeekNumber(String day) {
        return switch (day.toLowerCase()) {
            case "monday" -> 1;
            case "tuesday" -> 2;
            case "wednesday" -> 3;
            case "thursday" -> 4;
            case "friday" -> 5;
            case "saturday" -> 6;
            case "sunday" -> 7;
            default -> 1;
        };
    }

    public static boolean isReminderRequest(String text) {
        return text.toLowerCase().contains("remind me");
    }
}
