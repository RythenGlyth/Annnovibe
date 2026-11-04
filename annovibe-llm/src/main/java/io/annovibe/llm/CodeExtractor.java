package io.annovibe.llm;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Robustly strips markdown fences and explanatory text from LLM responses to extract pure Java code.
 */
public final class CodeExtractor {

    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```(?:java)?\\s*\\n?(.*?)\\n?```", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private CodeExtractor() {}

    /**
     * Extracts pure Java code from raw LLM response.
     */
    public static String extractCode(String response) {
        if (response == null || response.isBlank()) {
            return "";
        }

        // 1. Look for markdown code block ```java ... ```
        Matcher matcher = CODE_BLOCK_PATTERN.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // 2. Fallback: check if the whole response is code or if there are opening backticks
        String text = response.trim();
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            if (firstNewline != -1) {
                text = text.substring(firstNewline + 1);
            }
            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3);
            }
            return text.trim();
        }

        return text;
    }

    /**
     * Normalizes a method body so that it is properly wrapped in braces { ... }.
     */
    public static String normalizeMethodBody(String code) {
        String trimmed = extractCode(code).trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed;
        }
        return "{\n" + trimmed + "\n}";
    }
}
