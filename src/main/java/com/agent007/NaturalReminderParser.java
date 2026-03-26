package com.agent007;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NaturalReminderParser {
    
    public static class ParsedReminder {
        public final LocalDateTime dateTime;
        public final String message;
        
        public ParsedReminder(LocalDateTime dateTime, String message) {
            this.dateTime = dateTime;
            this.message = message;
        }
    }
    
    public static ParsedReminder parse(String text) {
        text = text.toLowerCase().trim();
        
        // Pattern: "remind me in X minutes/hours/days to/about ..."
        Pattern inPattern = Pattern.compile("remind me in (\\d+)\\s*(minute|minutes|min|hour|hours|hr|day|days)\\s*(?:to|about)?\\s*(.+)", Pattern.CASE_INSENSITIVE);
        Matcher inMatcher = inPattern.matcher(text);
        if (inMatcher.find()) {
            int amount = Integer.parseInt(inMatcher.group(1));
            String unit = inMatcher.group(2);
            String message = inMatcher.group(3);
            
            LocalDateTime remindAt = LocalDateTime.now();
            if (unit.startsWith("min")) {
                remindAt = remindAt.plusMinutes(amount);
            } else if (unit.startsWith("hour") || unit.equals("hr")) {
                remindAt = remindAt.plusHours(amount);
            } else if (unit.startsWith("day")) {
                remindAt = remindAt.plusDays(amount);
            }
            
            return new ParsedReminder(remindAt, message);
        }
        
        // Pattern: "remind me tomorrow at 3pm to/about ..."
        Pattern tomorrowPattern = Pattern.compile("remind me tomorrow\\s*(?:at)?\\s*(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\\s*(?:to|about)?\\s*(.+)", Pattern.CASE_INSENSITIVE);
        Matcher tomorrowMatcher = tomorrowPattern.matcher(text);
        if (tomorrowMatcher.find()) {
            int hour = Integer.parseInt(tomorrowMatcher.group(1));
            int minute = tomorrowMatcher.group(2) != null ? Integer.parseInt(tomorrowMatcher.group(2)) : 0;
            String ampm = tomorrowMatcher.group(3);
            String message = tomorrowMatcher.group(4);
            
            if (ampm != null && ampm.equalsIgnoreCase("pm") && hour < 12) {
                hour += 12;
            } else if (ampm != null && ampm.equalsIgnoreCase("am") && hour == 12) {
                hour = 0;
            }
            
            LocalDateTime remindAt = LocalDateTime.now().plusDays(1)
                .withHour(hour)
                .withMinute(minute)
                .withSecond(0);
            
            return new ParsedReminder(remindAt, message);
        }
        
        // Pattern: "remind me tomorrow to/about ..." (default 9am)
        Pattern tomorrowSimplePattern = Pattern.compile("remind me tomorrow\\s*(?:to|about)?\\s*(.+)", Pattern.CASE_INSENSITIVE);
        Matcher tomorrowSimpleMatcher = tomorrowSimplePattern.matcher(text);
        if (tomorrowSimpleMatcher.find()) {
            String message = tomorrowSimpleMatcher.group(1);
            LocalDateTime remindAt = LocalDateTime.now().plusDays(1)
                .withHour(9)
                .withMinute(0)
                .withSecond(0);
            
            return new ParsedReminder(remindAt, message);
        }
        
        // Pattern: "remind me at 3pm to/about ..."
        Pattern atTimePattern = Pattern.compile("remind me\\s*(?:at)?\\s*(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\\s*(?:to|about)?\\s*(.+)", Pattern.CASE_INSENSITIVE);
        Matcher atTimeMatcher = atTimePattern.matcher(text);
        if (atTimeMatcher.find()) {
            int hour = Integer.parseInt(atTimeMatcher.group(1));
            int minute = atTimeMatcher.group(2) != null ? Integer.parseInt(atTimeMatcher.group(2)) : 0;
            String ampm = atTimeMatcher.group(3);
            String message = atTimeMatcher.group(4);
            
            if (ampm != null && ampm.equalsIgnoreCase("pm") && hour < 12) {
                hour += 12;
            } else if (ampm != null && ampm.equalsIgnoreCase("am") && hour == 12) {
                hour = 0;
            }
            
            LocalDateTime remindAt = LocalDateTime.now()
                .withHour(hour)
                .withMinute(minute)
                .withSecond(0);
            
            // If time has passed today, schedule for tomorrow
            if (remindAt.isBefore(LocalDateTime.now())) {
                remindAt = remindAt.plusDays(1);
            }
            
            return new ParsedReminder(remindAt, message);
        }
        
        // Pattern: "remind me next week to/about ..." (default Monday 9am)
        Pattern nextWeekPattern = Pattern.compile("remind me next week\\s*(?:to|about)?\\s*(.+)", Pattern.CASE_INSENSITIVE);
        Matcher nextWeekMatcher = nextWeekPattern.matcher(text);
        if (nextWeekMatcher.find()) {
            String message = nextWeekMatcher.group(1);
            LocalDateTime remindAt = LocalDateTime.now().plusWeeks(1)
                .withHour(9)
                .withMinute(0)
                .withSecond(0);
            
            return new ParsedReminder(remindAt, message);
        }
        
        // Pattern: "remind me on monday/tuesday/etc at 3pm to/about ..."
        Pattern dayOfWeekPattern = Pattern.compile("remind me (?:on\\s+)?(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\s*(?:at)?\\s*(\\d{1,2})?(?::(\\d{2}))?\\s*(am|pm)?\\s*(?:to|about)?\\s*(.+)", Pattern.CASE_INSENSITIVE);
        Matcher dayOfWeekMatcher = dayOfWeekPattern.matcher(text);
        if (dayOfWeekMatcher.find()) {
            String dayName = dayOfWeekMatcher.group(1).toLowerCase();
            String hourStr = dayOfWeekMatcher.group(2);
            String minuteStr = dayOfWeekMatcher.group(3);
            String ampm = dayOfWeekMatcher.group(4);
            String message = dayOfWeekMatcher.group(5);
            
            int hour = hourStr != null ? Integer.parseInt(hourStr) : 9;
            int minute = minuteStr != null ? Integer.parseInt(minuteStr) : 0;
            
            if (ampm != null && ampm.equalsIgnoreCase("pm") && hour < 12) {
                hour += 12;
            } else if (ampm != null && ampm.equalsIgnoreCase("am") && hour == 12) {
                hour = 0;
            }
            
            int targetDay = getDayOfWeekNumber(dayName);
            int currentDay = LocalDateTime.now().getDayOfWeek().getValue();
            int daysToAdd = (targetDay - currentDay + 7) % 7;
            if (daysToAdd == 0) daysToAdd = 7; // Next week if same day
            
            LocalDateTime remindAt = LocalDateTime.now().plusDays(daysToAdd)
                .withHour(hour)
                .withMinute(minute)
                .withSecond(0);
            
            return new ParsedReminder(remindAt, message);
        }
        
        return null;
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
