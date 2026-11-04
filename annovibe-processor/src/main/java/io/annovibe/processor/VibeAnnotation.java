package io.annovibe.processor;

import io.annovibe.AI;
import io.annovibe.vibe;

import javax.lang.model.element.Element;

/**
 * Normalizes metadata extracted from either @AI or @vibe.
 */
public record VibeAnnotation(
        String prompt,
        String model,
        String provider,
        double temperature,
        boolean cache,
        String systemPrompt
) {
    public static VibeAnnotation fromElement(Element element) {
        AI ai = element.getAnnotation(AI.class);
        if (ai != null) {
            return new VibeAnnotation(
                    ai.value(),
                    ai.model(),
                    ai.provider(),
                    ai.temperature(),
                    ai.cache(),
                    ai.systemPrompt()
            );
        }
        vibe v = element.getAnnotation(vibe.class);
        if (v != null) {
            return new VibeAnnotation(
                    v.value(),
                    v.model(),
                    v.provider(),
                    v.temperature(),
                    v.cache(),
                    v.systemPrompt()
            );
        }
        return null;
    }
}
