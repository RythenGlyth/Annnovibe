package io.annovibe.llm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CodeExtractorTest {

    @Test
    public void testExtractFromMarkdown() {
        String input = """
                Here is the implementation:
                ```java
                if (n <= 1) return 1;
                return n * factorial(n - 1);
                ```
                Hope this helps!
                """;
        String extracted = CodeExtractor.extractCode(input);
        assertEquals("if (n <= 1) return 1;\nreturn n * factorial(n - 1);", extracted);
    }

    @Test
    public void testNormalizeMethodBody() {
        String body = "return a + b;";
        String normalized = CodeExtractor.normalizeMethodBody(body);
        assertEquals("{\nreturn a + b;\n}", normalized);

        String alreadyWrapped = "{\n    return a + b;\n}";
        assertEquals(alreadyWrapped, CodeExtractor.normalizeMethodBody(alreadyWrapped));
    }

    @Test
    public void testMarkdownWithoutLanguageTag() {
        String input = "```\nreturn Math.max(a, b);\n```";
        assertEquals("return Math.max(a, b);", CodeExtractor.extractCode(input));
    }
}
