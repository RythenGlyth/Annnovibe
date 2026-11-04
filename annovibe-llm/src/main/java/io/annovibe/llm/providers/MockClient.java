package io.annovibe.llm.providers;

import io.annovibe.llm.LlmClient;
import io.annovibe.llm.LlmProvider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Mock LLM client for offline unit testing, CI/CD, and fast local verification.
 */
public class MockClient implements LlmClient {

    private static final Map<String, String> STATIC_RESPONSES = new ConcurrentHashMap<>();
    private static Function<String, String> dynamicResponder = null;

    public static void registerResponse(String promptSubstring, String javaCode) {
        STATIC_RESPONSES.put(promptSubstring, javaCode);
    }

    public static void setDynamicResponder(Function<String, String> responder) {
        dynamicResponder = responder;
    }

    public static void clear() {
        STATIC_RESPONSES.clear();
        dynamicResponder = null;
    }

    @Override
    public LlmProvider getProvider() {
        return LlmProvider.MOCK;
    }

    @Override
    public String complete(String systemPrompt, String userPrompt, double temperature) {
        if (dynamicResponder != null) {
            String res = dynamicResponder.apply(userPrompt);
            if (res != null) return res;
        }

        for (Map.Entry<String, String> entry : STATIC_RESPONSES.entrySet()) {
            if (userPrompt.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        // Check for 1-shot self-healing correction prompt
        if (userPrompt.contains("CRITICAL: The previous Java code you generated produced a compilation/syntax error")) {
            return "{\n    return 777;\n}";
        }

        // Extract the specific requirement section to avoid false positives on sibling methods in context
        String req = userPrompt;
        int reqIdx = userPrompt.indexOf("Requirement / Vibe Prompt:");
        if (reqIdx != -1) {
            req = userPrompt.substring(reqIdx);
        }
        String lowerReq = req.toLowerCase();

        // Default smart heuristic fallback if not explicitly registered:
        if (lowerReq.contains("simulate syntax error then self heal")) {
            return "{\n    this is intentional bad syntax !!!\n}";
        }
        if (lowerReq.contains("maximum of two") || lowerReq.contains("max of two")) {
            return "{\n    return Math.max(a, b);\n}";
        }
        if (lowerReq.contains("factorial")) {
            return "{\n    if (n <= 1) return 1L;\n    return n * factorial(n - 1);\n}";
        }
        if (lowerReq.contains("reverse")) {
            return "{\n    if (input == null) return \"\";\n    return new StringBuilder(input).reverse().toString();\n}";
        }
        if (lowerReq.contains("palindrome")) {
            return "{\n    if (str == null) return false;\n    String clean = str.replaceAll(\"\\\\s+\", \"\").toLowerCase();\n    return clean.equals(new StringBuilder(clean).reverse().toString());\n}";
        }
        if (lowerReq.contains("sum") || lowerReq.contains("add")) {
            return "{\n    return a + b;\n}";
        }
        if (lowerReq.contains("create a new user")) {
            return "{\n    User u = new User(id, email);\n    return repo.save(u);\n}";
        }
        if (lowerReq.contains("into store") || lowerReq.contains("save user into store")) {
            return "{\n    store.put(user.getId(), user);\n    return user;\n}";
        }
        if (lowerReq.contains("find and return user")) {
            return "{\n    return store.get(id);\n}";
        }

        return "{\n    throw new UnsupportedOperationException(\"Mock response for prompt: " + userPrompt.replace("\"", "\\\"") + "\");\n}";
    }
}
