package com.agent007;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class OllamaClient {
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String model = "llama3.2";

    public String chat(String context, String message) {
        try {
            String prompt = context.isEmpty() ? message : context + "\n\nUser: " + message;
            
            Map<String, Object> payload = Map.of(
                "model", model,
                "prompt", prompt,
                "stream", false
            );

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:11434/api/generate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            Map<String, Object> result = mapper.readValue(response.body(), Map.class);
            return (String) result.get("response");
        } catch (Exception e) {
            return "Error connecting to Ollama: " + e.getMessage();
        }
    }
}
