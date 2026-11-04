package io.annovibe.llm;

import io.annovibe.llm.json.SimpleJson;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class SimpleJsonTest {

    @Test
    public void testSerialization() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", "Annovibe");
        map.put("version", 1);
        map.put("active", true);
        map.put("tags", List.of("ai", "compiler", "vibe"));

        String json = SimpleJson.stringify(map);
        assertTrue(json.contains("\"name\":\"Annovibe\""));
        assertTrue(json.contains("\"version\":1"));
        assertTrue(json.contains("\"active\":true"));
        assertTrue(json.contains("[\"ai\",\"compiler\",\"vibe\"]"));
    }

    @Test
    public void testParseObject() {
        String json = "{\"model\": \"gpt-4o\", \"temp\": 0.5, \"active\": true, \"nullVal\": null, \"list\": [1, 2, \"three\"]}";
        Map<String, Object> map = SimpleJson.parseObject(json);

        assertEquals("gpt-4o", map.get("model"));
        assertEquals(0.5, (Double) map.get("temp"), 0.001);
        assertEquals(Boolean.TRUE, map.get("active"));
        assertNull(map.get("nullVal"));

        List<?> list = (List<?>) map.get("list");
        assertEquals(3, list.size());
        assertEquals(1L, list.get(0));
        assertEquals("three", list.get(2));
    }

    @Test
    public void testEscapeSequences() {
        String json = "{\"code\": \"public void test() {\\n\\tSystem.out.println(\\\"Hello\\\\n\\\");\\n}\"}";
        Map<String, Object> map = SimpleJson.parseObject(json);
        String code = (String) map.get("code");
        assertTrue(code.contains("public void test()"));
        assertTrue(code.contains("\"Hello\\n\""));
    }
}
