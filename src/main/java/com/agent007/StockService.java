package com.agent007;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class StockService {
    private final HttpClient client;
    private final ObjectMapper mapper;

    public StockService() {
        this.client = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();
    }

    public String getStockQuote(String symbol) {
        try {
            // Using Yahoo Finance API alternative - finnhub.io (free tier)
            // For production, you'd want to use a proper API with key
            // Using a free public API for now
            String url = String.format(
                "https://query1.finance.yahoo.com/v8/finance/chart/%s?interval=1d&range=1d",
                symbol.toUpperCase()
            );
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0")
                .GET()
                .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode data = mapper.readTree(response.body());
            
            if (data.has("chart") && data.get("chart").has("result") && 
                !data.get("chart").get("result").isEmpty()) {
                
                JsonNode result = data.get("chart").get("result").get(0);
                JsonNode meta = result.get("meta");
                
                double currentPrice = meta.get("regularMarketPrice").asDouble();
                double previousClose = meta.get("chartPreviousClose").asDouble();
                double change = currentPrice - previousClose;
                double changePercent = (change / previousClose) * 100;
                
                String emoji = change >= 0 ? "📈" : "📉";
                String changeStr = String.format("%+.2f (%+.2f%%)", change, changePercent);
                
                return String.format("%s %s: $%.2f %s", 
                    emoji, symbol.toUpperCase(), currentPrice, changeStr);
            }
            
            return "❌ Unable to fetch data for " + symbol.toUpperCase();
            
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return "❌ Error fetching " + symbol.toUpperCase();
        }
    }

    public String getMultipleStocks(List<String> symbols) {
        StringBuilder report = new StringBuilder("📊 Stock Market Update\n\n");
        
        for (String symbol : symbols) {
            String quote = getStockQuote(symbol);
            report.append(quote).append("\n");
        }
        
        return report.toString();
    }

    public boolean isMarketOpen() {
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now(
            java.time.ZoneId.of("America/New_York")
        );
        
        // Check if it's a weekday (Monday-Friday)
        java.time.DayOfWeek dayOfWeek = now.getDayOfWeek();
        if (dayOfWeek == java.time.DayOfWeek.SATURDAY || dayOfWeek == java.time.DayOfWeek.SUNDAY) {
            return false;
        }
        
        // Check if it's during market hours (9:30 AM - 4:00 PM ET)
        int hour = now.getHour();
        int minute = now.getMinute();
        int timeInMinutes = hour * 60 + minute;
        
        int marketOpen = 9 * 60 + 30;  // 9:30 AM
        int marketClose = 16 * 60;      // 4:00 PM
        
        return timeInMinutes >= marketOpen && timeInMinutes < marketClose;
    }
}
