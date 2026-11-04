package io.annovibe.llm;

/**
 * Unified interface for executing LLM completions.
 */
public interface LlmClient {

    /**
     * Generates a completion given a system instruction and user prompt.
     *
     * @param systemPrompt System instructions for the model
     * @param userPrompt The user prompt containing code context and task description
     * @param temperature Sampling temperature
     * @return Raw text completion from the LLM
     * @throws Exception if network, authentication, or parsing fails
     */
    String complete(String systemPrompt, String userPrompt, double temperature) throws Exception;

    /**
     * Gets the provider type for this client.
     */
    LlmProvider getProvider();
}
