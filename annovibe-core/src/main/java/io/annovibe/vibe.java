package io.annovibe;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Triggers compile-time generation of the method body or class contents using an LLM.
 * Embraces the vibe coding aesthetic with lowercase naming.
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface vibe {
    /**
     * The vibe / prompt describing the desired functionality.
     */
    String value();

    /**
     * Optional model override (e.g. "gpt-4o", "claude-3-5-sonnet-20241022", "qwen2.5-coder").
     */
    String model() default "";

    /**
     * Optional provider override ("openrouter", "openai", "anthropic", "ollama", "mock").
     */
    String provider() default "";

    /**
     * Sampling temperature for the model generation. Default is 0.2.
     */
    double temperature() default 0.2;

    /**
     * Whether to use compile-time caching for this element. Default is true.
     */
    boolean cache() default true;

    /**
     * Optional system prompt instructions specific to this element.
     */
    String systemPrompt() default "";
}
