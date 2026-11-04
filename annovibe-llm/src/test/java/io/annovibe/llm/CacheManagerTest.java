package io.annovibe.llm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class CacheManagerTest {

    @Test
    public void testCacheHitAndMiss(@TempDir Path tempDir) {
        CacheManager cache = new CacheManager(tempDir, true);
        String hash = CacheManager.computeHash("openai", "gpt-4o", "int add(int a, int b)", "sum numbers");

        assertEquals(Optional.empty(), cache.get(hash));

        String code = "{\n    return a + b;\n}";
        cache.put(hash, code);

        Optional<String> cached = cache.get(hash);
        assertTrue(cached.isPresent());
        assertEquals(code, cached.get());
    }

    @Test
    public void testDisabledCache(@TempDir Path tempDir) {
        CacheManager cache = new CacheManager(tempDir, false);
        String hash = CacheManager.computeHash("test");
        cache.put(hash, "code");
        assertTrue(cache.get(hash).isEmpty());
    }
}
