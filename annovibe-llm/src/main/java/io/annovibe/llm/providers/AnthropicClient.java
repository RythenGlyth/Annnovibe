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
 * Client for Anthropic Messages API.
 */
public class AnthropicClient implements LlmClient {

    private final LlmConfig config;
    private final HttpClient httpClient;

    public AnthropicClient(LlmConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    @Override
    public LlmProvider getProvider() {
        return LlmProvider.ANTHROPIC;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String complete(String systemPrompt, String userPrompt, double temperature) throws Exception {
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new IllegalStateException("Anthropic API key is missing. Set ANTHROPIC_API_KEY environment variable or configure annovibe.properties.");
        }

        Map<String, Object> reqBody = new LinkedHashMap<>();
        reqBody.put("model", config.getModel());
        reqBody.put("max_tokens", 4096);
        reqBody.put("temperature", temperature);
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            reqBody.put("system", systemPrompt);
        }

        List<Map<String, String>> messages = List.of(Map.of("role", "user", "content", userPrompt));
        reqBody.put("messages", messages);

        String jsonPayload = SimpleJson.stringify(reqBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getBaseUrl()))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("x-api-key", config.getApiKey())
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("Anthropic API call failed [HTTP " + response.statusCode() + "]: " + response.body());
        }

        Map<String, Object> respMap = SimpleJson.parseObject(response.body());
        List<Object> contentList = (List<Object>) respMap.get("content");
        if (contentList == null || contentList.isEmpty()) {
            throw new RuntimeException("No content returned from Anthropic: " + response.body());
        }

        StringBuilder sb = new StringBuilder();
        for (Object item : contentList) {
            if (item instanceof Map<?, ?> itemMap) {
                Object text = itemMap.get("text");
                if (text != null) {
                    sb.append(text);
                }
            }
        }

        return sb.toString();
    }
}
