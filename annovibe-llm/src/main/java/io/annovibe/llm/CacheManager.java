package io.annovibe.llm;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

/**
 * Manages compile-time caching on disk to avoid redundant LLM invocations and enable deterministic builds.
 */
public class CacheManager {

    private final Path cacheDir;
    private final boolean enabled;

    public CacheManager(Path cacheDir, boolean enabled) {
        this.cacheDir = cacheDir;
        this.enabled = enabled;
        if (enabled) {
            try {
                Files.createDirectories(cacheDir);
            } catch (IOException ignored) {}
        }
    }

    public static String computeHash(String... parts) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String part : parts) {
                if (part != null) {
                    digest.update(part.getBytes(StandardCharsets.UTF_8));
                    digest.update((byte) 0);
                }
            }
            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public Optional<String> get(String hash) {
        if (!enabled || hash == null) return Optional.empty();
        Path file = cacheDir.resolve(hash + ".java");
        if (Files.exists(file)) {
            try {
                return Optional.of(Files.readString(file, StandardCharsets.UTF_8));
            } catch (IOException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public void put(String hash, String code) {
        if (!enabled || hash == null || code == null) return;
        Path file = cacheDir.resolve(hash + ".java");
        try {
            Files.createDirectories(cacheDir);
            Files.writeString(file, code, StandardCharsets.UTF_8);
        } catch (IOException ignored) {}
    }

    public void clear() {
        if (Files.exists(cacheDir)) {
            try (var stream = Files.walk(cacheDir)) {
                stream.filter(Files::isRegularFile).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {}
                });
            } catch (IOException ignored) {}
        }
    }
}
