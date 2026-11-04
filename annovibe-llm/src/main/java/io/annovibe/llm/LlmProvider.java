package io.annovibe.llm;

/**
 * Supported LLM providers.
 */
public enum LlmProvider {
    OPENROUTER("openrouter"),
    OPENAI("openai"),
    ANTHROPIC("anthropic"),
    MOCK("mock");

    private final String key;

    LlmProvider(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public static LlmProvider fromString(String val) {
        if (val == null || val.isBlank()) return null;
        String clean = val.trim().toLowerCase();
        for (LlmProvider p : values()) {
            if (p.key.equals(clean) || p.name().toLowerCase().equals(clean)) {
                return p;
            }
        }
        return null;
    }
}
