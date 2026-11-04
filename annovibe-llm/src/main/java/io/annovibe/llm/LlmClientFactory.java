package io.annovibe.llm;

import io.annovibe.llm.providers.AnthropicClient;
import io.annovibe.llm.providers.MockClient;
import io.annovibe.llm.providers.OllamaClient;
import io.annovibe.llm.providers.OpenAiClient;
import io.annovibe.llm.providers.OpenRouterClient;

/**
 * Factory for creating configured LLM clients.
 */
public final class LlmClientFactory {

    private LlmClientFactory() {}

    public static LlmClient create(LlmConfig config) {
        if (config.getProvider() == null) {
            return new OpenRouterClient(config);
        }
        return switch (config.getProvider()) {
            case OPENAI -> new OpenAiClient(config);
            case ANTHROPIC -> new AnthropicClient(config);
            case OPENROUTER -> new OpenRouterClient(config);
            case OLLAMA -> new OllamaClient(config);
            case MOCK -> new MockClient();
        };
    }
}
