package com.agent007;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class WeatherService {
    private final HttpClient client;
    private final ObjectMapper mapper;

    public WeatherService() {
        this.client = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();
    }

    public String getWeather(String location) {
        try {
            // Using Open-Meteo API (free, no API key needed)
            // First, geocode the location
            String geocodeUrl = String.format(
                "https://geocoding-api.open-meteo.com/v1/search?name=%s&count=1&language=en&format=json",
                location.replace(" ", "%20")
            );
            
            HttpRequest geocodeRequest = HttpRequest.newBuilder()
                .uri(URI.create(geocodeUrl))
                .GET()
                .build();
            
            HttpResponse<String> geocodeResponse = client.send(geocodeRequest, HttpResponse.BodyHandlers.ofString());
            JsonNode geocodeData = mapper.readTree(geocodeResponse.body());
            
            if (!geocodeData.has("results") || geocodeData.get("results").isEmpty()) {
                return "❌ Location not found: " + location;
            }
            
            JsonNode locationData = geocodeData.get("results").get(0);
            double lat = locationData.get("latitude").asDouble();
            double lon = locationData.get("longitude").asDouble();
            String name = locationData.get("name").asText();
            String country = locationData.has("country") ? locationData.get("country").asText() : "";
            
            // Get weather data
            String weatherUrl = String.format(
                "https://api.open-meteo.com/v1/forecast?latitude=%.2f&longitude=%.2f&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m&hourly=temperature_2m,weather_code&daily=temperature_2m_max,temperature_2m_min,weather_code&timezone=auto",
                lat, lon
            );
            
            HttpRequest weatherRequest = HttpRequest.newBuilder()
                .uri(URI.create(weatherUrl))
                .GET()
                .build();
            
            HttpResponse<String> weatherResponse = client.send(weatherRequest, HttpResponse.BodyHandlers.ofString());
            JsonNode weatherData = mapper.readTree(weatherResponse.body());
            
            return formatWeather(name, country, weatherData);
            
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return "❌ Failed to fetch weather data";
        }
    }

    private String formatWeather(String location, String country, JsonNode data) {
        JsonNode current = data.get("current");
        JsonNode daily = data.get("daily");
        
        double temp = current.get("temperature_2m").asDouble();
        int humidity = current.get("relative_humidity_2m").asInt();
        double windSpeed = current.get("wind_speed_10m").asDouble();
        int weatherCode = current.get("weather_code").asInt();
        
        double maxTemp = daily.get("temperature_2m_max").get(0).asDouble();
        double minTemp = daily.get("temperature_2m_min").get(0).asDouble();
        
        String condition = getWeatherCondition(weatherCode);
        String emoji = getWeatherEmoji(weatherCode);
        
        return String.format("""
            %s Weather for %s, %s
            
            🌡️ Current: %.1f°C
            📊 High: %.1f°C | Low: %.1f°C
            💧 Humidity: %d%%
            💨 Wind: %.1f km/h
            %s %s
            """, emoji, location, country, temp, maxTemp, minTemp, humidity, windSpeed, emoji, condition);
    }

    public String getHourlyForecast(String location) {
        try {
            String geocodeUrl = String.format(
                "https://geocoding-api.open-meteo.com/v1/search?name=%s&count=1&language=en&format=json",
                location.replace(" ", "%20")
            );
            
            HttpRequest geocodeRequest = HttpRequest.newBuilder()
                .uri(URI.create(geocodeUrl))
                .GET()
                .build();
            
            HttpResponse<String> geocodeResponse = client.send(geocodeRequest, HttpResponse.BodyHandlers.ofString());
            JsonNode geocodeData = mapper.readTree(geocodeResponse.body());
            
            if (!geocodeData.has("results") || geocodeData.get("results").isEmpty()) {
                return "❌ Location not found: " + location;
            }
            
            JsonNode locationData = geocodeData.get("results").get(0);
            double lat = locationData.get("latitude").asDouble();
            double lon = locationData.get("longitude").asDouble();
            
            String weatherUrl = String.format(
                "https://api.open-meteo.com/v1/forecast?latitude=%.2f&longitude=%.2f&hourly=temperature_2m,weather_code&forecast_hours=24&timezone=auto",
                lat, lon
            );
            
            HttpRequest weatherRequest = HttpRequest.newBuilder()
                .uri(URI.create(weatherUrl))
                .GET()
                .build();
            
            HttpResponse<String> weatherResponse = client.send(weatherRequest, HttpResponse.BodyHandlers.ofString());
            JsonNode weatherData = mapper.readTree(weatherResponse.body());
            
            return formatHourlyForecast(weatherData);
            
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return "❌ Failed to fetch hourly forecast";
        }
    }

    private String formatHourlyForecast(JsonNode data) {
        JsonNode hourly = data.get("hourly");
        JsonNode times = hourly.get("time");
        JsonNode temps = hourly.get("temperature_2m");
        JsonNode codes = hourly.get("weather_code");
        
        StringBuilder forecast = new StringBuilder("📅 24-Hour Forecast:\n\n");
        
        for (int i = 0; i < Math.min(24, times.size()); i += 3) {
            String time = times.get(i).asText().substring(11, 16);
            double temp = temps.get(i).asDouble();
            int code = codes.get(i).asInt();
            String emoji = getWeatherEmoji(code);
            
            forecast.append(String.format("%s %s: %.1f°C %s\n", emoji, time, temp, getWeatherCondition(code)));
        }
        
        return forecast.toString();
    }

    private String getWeatherCondition(int code) {
        return switch (code) {
            case 0 -> "Clear sky";
            case 1, 2, 3 -> "Partly cloudy";
            case 45, 48 -> "Foggy";
            case 51, 53, 55 -> "Drizzle";
            case 61, 63, 65 -> "Rain";
            case 71, 73, 75 -> "Snow";
            case 77 -> "Snow grains";
            case 80, 81, 82 -> "Rain showers";
            case 85, 86 -> "Snow showers";
            case 95 -> "Thunderstorm";
            case 96, 99 -> "Thunderstorm with hail";
            default -> "Unknown";
        };
    }

    private String getWeatherEmoji(int code) {
        return switch (code) {
            case 0 -> "☀️";
            case 1, 2, 3 -> "⛅";
            case 45, 48 -> "🌫️";
            case 51, 53, 55, 61, 63, 65, 80, 81, 82 -> "🌧️";
            case 71, 73, 75, 77, 85, 86 -> "❄️";
            case 95, 96, 99 -> "⛈️";
            default -> "🌤️";
        };
    }
}
