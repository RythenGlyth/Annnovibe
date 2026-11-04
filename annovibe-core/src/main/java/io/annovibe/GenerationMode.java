package io.annovibe;

/**
 * Defines the generation target mode for Annovibe.
 */
public enum GenerationMode {
    /**
     * Injects the generated code directly into the compiler AST in-memory.
     * The compiled .class bytecode contains the implementation.
     */
    AST_INJECT,

    /**
     * Replaces the stub implementation directly in the .java source file on disk.
     */
    MATERIALIZE,

    /**
     * Both in-memory AST injection and source code materialization.
     */
    ALL
}
