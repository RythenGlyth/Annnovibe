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
 * Client for local Ollama instances (zero-cost, offline AI code generation).
 * Defaults to http://localhost:11434.
 */
public class OllamaClient implements LlmClient {

    private final LlmConfig config;
    private final HttpClient httpClient;

    public OllamaClient(LlmConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public LlmProvider getProvider() {
        return LlmProvider.OLLAMA;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String complete(String systemPrompt, String userPrompt, double temperature) throws Exception {
        List<Map<String, String>> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        messages.add(Map.of("role", "user", "content", userPrompt));

        Map<String, Object> reqBody = new LinkedHashMap<>();
        reqBody.put("model", config.getModel());
        reqBody.put("messages", messages);
        reqBody.put("stream", false);
        reqBody.put("options", Map.of("temperature", temperature));

        String jsonPayload = SimpleJson.stringify(reqBody);

        String targetUrl = config.getBaseUrl();
        if (targetUrl == null || targetUrl.isBlank()) {
            targetUrl = "http://localhost:11434/api/chat";
        } else if (!targetUrl.contains("/api/chat") && !targetUrl.contains("/v1/chat/completions")) {
            if (targetUrl.endsWith("/")) targetUrl += "api/chat";
            else targetUrl += "/api/chat";
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(targetUrl))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("Ollama call failed [HTTP " + response.statusCode() + "]: " + response.body());
        }

        Map<String, Object> respMap = SimpleJson.parseObject(response.body());

        // Format 1: Native Ollama /api/chat {"message": {"content": "..."}}
        if (respMap.get("message") instanceof Map<?, ?> msgMap && msgMap.get("content") != null) {
            return msgMap.get("content").toString();
        }

        // Format 2: OpenAI compatible {"choices": [{"message": {"content": "..."}}]}
        if (respMap.get("choices") instanceof List<?> choices && !choices.isEmpty()) {
            if (choices.get(0) instanceof Map<?, ?> firstChoice && firstChoice.get("message") instanceof Map<?, ?> msg) {
                if (msg.get("content") != null) {
                    return msg.get("content").toString();
                }
            }
        }

        // Format 3: Raw response {"response": "..."}
        if (respMap.get("response") != null) {
            return respMap.get("response").toString();
        }

        throw new RuntimeException("Unexpected response format from Ollama: " + response.body());
    }
}
