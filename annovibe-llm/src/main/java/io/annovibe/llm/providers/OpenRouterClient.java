package io.annovibe.llm.providers;

import io.annovibe.llm.LlmClient;
import io.annovibe.llm.LlmConfig;
import io.annovibe.llm.LlmProvider;
import io.annovibe.llm.json.SimpleJson;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * Client for OpenRouter API.
 */
public class OpenRouterClient implements LlmClient {

    private final LlmConfig config;
    private final HttpClient httpClient;

    public OpenRouterClient(LlmConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    @Override
    public LlmProvider getProvider() {
        return LlmProvider.OPENROUTER;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String complete(String systemPrompt, String userPrompt, double temperature) throws Exception {
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new IllegalStateException("OpenRouter API key is missing. Set OPENROUTER_API_KEY environment variable or configure annovibe.properties.");
        }

        List<Map<String, String>> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        messages.add(Map.of("role", "user", "content", userPrompt));

        Map<String, Object> reqBody = new LinkedHashMap<>();
        reqBody.put("model", config.getModel());
        reqBody.put("temperature", temperature);
        reqBody.put("messages", messages);

        String jsonPayload = SimpleJson.stringify(reqBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getBaseUrl()))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + config.getApiKey())
                .header("HTTP-Referer", "https://github.com/annovibe/annovibe")
                .header("X-Title", "Annovibe")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("OpenRouter API call failed [HTTP " + response.statusCode() + "]: " + response.body());
        }

        Map<String, Object> respMap = SimpleJson.parseObject(response.body());
        List<Object> choices = (List<Object>) respMap.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("No choices returned from OpenRouter: " + response.body());
        }

        Map<String, Object> firstChoice = (Map<String, Object>) choices.get(0);
        Map<String, Object> msg = (Map<String, Object>) firstChoice.get("message");
        if (msg == null || msg.get("content") == null) {
            throw new RuntimeException("No message content in OpenRouter response: " + response.body());
        }

        return msg.get("content").toString();
    }
}
