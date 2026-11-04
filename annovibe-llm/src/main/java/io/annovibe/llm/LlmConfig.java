package io.annovibe.llm;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Configuration holder for LLM execution in Annovibe.
 * Resolves settings hierarchically from:
 * 1. Annotation parameters
 * 2. Compiler options (-Aannovibe.*)
 * 3. annovibe.properties file in working directory
 * 4. System properties (-Dannovibe.*)
 * 5. Environment variables (OPENROUTER_API_KEY, OPENAI_API_KEY, ANTHROPIC_API_KEY)
 */
public class LlmConfig {

    private final LlmProvider provider;
    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final double temperature;
    private final boolean cacheEnabled;
    private final Path cacheDir;
    private final String globalPrompt;

    public LlmConfig(LlmProvider provider, String apiKey, String model, String baseUrl, double temperature, boolean cacheEnabled, Path cacheDir, String globalPrompt) {
        this.provider = provider;
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
        this.temperature = temperature;
        this.cacheEnabled = cacheEnabled;
        this.cacheDir = cacheDir;
        this.globalPrompt = globalPrompt;
    }

    public LlmProvider getProvider() {
        return provider;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getModel() {
        return model;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public double getTemperature() {
        return temperature;
    }

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public Path getCacheDir() {
        return cacheDir;
    }

    public String getGlobalPrompt() {
        return globalPrompt;
    }

    /**
     * Resolves configuration by blending compile options, file, system properties, and env vars.
     */
    public static LlmConfig resolve(String annotationProvider, String annotationModel, Double annotationTemp, Boolean annotationCache) {
        Properties fileProps = loadFileProperties();

        // 1. Resolve Provider
        LlmProvider prov = LlmProvider.fromString(annotationProvider);
        if (prov == null) {
            String provStr = getVal("annovibe.provider", fileProps, "ANNOVIBE_PROVIDER");
            prov = LlmProvider.fromString(provStr);
        }
        if (prov == null) {
            // Auto-detect based on available API keys
            if (hasEnvOrProp("OPENROUTER_API_KEY", "annovibe.openrouter.apiKey", fileProps)) {
                prov = LlmProvider.OPENROUTER;
            } else if (hasEnvOrProp("OPENAI_API_KEY", "annovibe.openai.apiKey", fileProps)) {
                prov = LlmProvider.OPENAI;
            } else if (hasEnvOrProp("ANTHROPIC_API_KEY", "annovibe.anthropic.apiKey", fileProps)) {
                prov = LlmProvider.ANTHROPIC;
            } else {
                prov = LlmProvider.OPENROUTER; // default fallback
            }
        }

        // 2. Resolve API Key for chosen provider
        String key = null;
        switch (prov) {
            case OPENROUTER -> key = getVal("annovibe.openrouter.apiKey", fileProps, "OPENROUTER_API_KEY", "ANNOVIBE_API_KEY");
            case OPENAI -> key = getVal("annovibe.openai.apiKey", fileProps, "OPENAI_API_KEY", "ANNOVIBE_API_KEY");
            case ANTHROPIC -> key = getVal("annovibe.anthropic.apiKey", fileProps, "ANTHROPIC_API_KEY", "ANNOVIBE_API_KEY");
            case MOCK -> key = "mock-key";
        }

        // 3. Resolve Model
        String mdl = (annotationModel != null && !annotationModel.isBlank()) ? annotationModel : null;
        if (mdl == null) {
            mdl = getVal("annovibe.model", fileProps, "ANNOVIBE_MODEL");
        }
        if (mdl == null) {
            mdl = switch (prov) {
                case OPENROUTER -> "anthropic/claude-3.5-sonnet";
                case OPENAI -> "gpt-4o-mini";
                case ANTHROPIC -> "claude-3-5-sonnet-20241022";
                case MOCK -> "mock-model";
            };
        }

        // 4. Resolve Base URL
        String url = getVal("annovibe.baseUrl", fileProps, "ANNOVIBE_BASE_URL");
        if (url == null || url.isBlank()) {
            url = switch (prov) {
                case OPENROUTER -> "https://openrouter.ai/api/v1/chat/completions";
                case OPENAI -> "https://api.openai.com/v1/chat/completions";
                case ANTHROPIC -> "https://api.anthropic.com/v1/messages";
                case MOCK -> "http://localhost:0/mock";
            };
        }

        // 5. Temperature
        double temp = (annotationTemp != null) ? annotationTemp : 0.2;
        String tempStr = getVal("annovibe.temperature", fileProps, "ANNOVIBE_TEMPERATURE");
        if (tempStr != null) {
            try {
                temp = Double.parseDouble(tempStr);
            } catch (NumberFormatException ignored) {}
        }

        // 6. Cache
        boolean cache = (annotationCache != null) ? annotationCache : true;
        String cacheStr = getVal("annovibe.cache", fileProps, "ANNOVIBE_CACHE");
        if (cacheStr != null) {
            cache = Boolean.parseBoolean(cacheStr);
        }

        // 7. Cache Directory
        String dirStr = getVal("annovibe.cacheDir", fileProps, "ANNOVIBE_CACHE_DIR");
        Path cacheDir = (dirStr != null && !dirStr.isBlank()) ? Paths.get(dirStr) : Paths.get(".annovibe", "cache");

        // 8. Global Prompt
        String globalPrompt = getVal("annovibe.globalPrompt", fileProps, "ANNOVIBE_GLOBAL_PROMPT");

        return new LlmConfig(prov, key, mdl, url, temp, cache, cacheDir, globalPrompt);
    }

    private static boolean hasEnvOrProp(String envName, String propName, Properties props) {
        String v = System.getenv(envName);
        if (v != null && !v.isBlank()) return true;
        v = System.getProperty(propName);
        if (v != null && !v.isBlank()) return true;
        if (props != null) {
            v = props.getProperty(propName);
            return v != null && !v.isBlank();
        }
        return false;
    }

    private static String getVal(String propKey, Properties props, String... envKeys) {
        String val = System.getProperty(propKey);
        if (val != null && !val.isBlank()) return val.trim();
        if (props != null) {
            val = props.getProperty(propKey);
            if (val != null && !val.isBlank()) return val.trim();
        }
        for (String envKey : envKeys) {
            val = System.getenv(envKey);
            if (val != null && !val.isBlank()) return val.trim();
        }
        return null;
    }

    private static Properties loadFileProperties() {
        Properties props = new Properties();
        File f = new File("annovibe.properties");
        if (f.exists() && f.isFile()) {
            try (InputStream in = new FileInputStream(f)) {
                props.load(in);
            } catch (Exception ignored) {}
        }
        return props;
    }
}
