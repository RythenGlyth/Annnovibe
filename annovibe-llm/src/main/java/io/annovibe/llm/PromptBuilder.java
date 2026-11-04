package io.annovibe.llm;

import java.util.List;

/**
 * Constructs contextual prompts for generating method bodies and class definitions.
 * Incorporates sibling class contracts and enclosing class symbols to ensure cross-component coherence.
 */
public class PromptBuilder {

    private static final String DEFAULT_SYSTEM_PROMPT = """
            You are an expert Java compiler code generation engine.
            Your task is to generate clean, highly efficient, and idiomatically sound Java code.
            RULES:
            1. Output ONLY pure Java code.
            2. Do NOT output any explanations, markdown fences (no ```), or conversational filler.
            3. For a method body, output only the statement block starting with '{' and ending with '}'.
            4. For class members, output only valid Java member declarations (fields, constructors, methods).
            5. Strictly adhere to the given method signatures, return types, parameter names, and imported types.
            6. When sibling methods or classes are provided in the context, reuse and invoke them appropriately.
            """;

    public static String buildSystemPrompt(String customPrompt, String globalPrompt) {
        StringBuilder sb = new StringBuilder(DEFAULT_SYSTEM_PROMPT);
        if (globalPrompt != null && !globalPrompt.isBlank()) {
            sb.append("\nPROJECT-WIDE VIBE & CONVENTIONS:\n").append(globalPrompt).append("\n");
        }
        if (customPrompt != null && !customPrompt.isBlank()) {
            sb.append("\nSPECIFIC DIRECTIVE:\n").append(customPrompt).append("\n");
        }
        return sb.toString();
    }

    /**
     * Builds a prompt for generating a method's implementation body.
     */
    public static String buildMethodPrompt(
            String prompt,
            String packageName,
            List<String> imports,
            String className,
            String classFieldsAndExistingMethods,
            String methodSignature,
            String siblingContext) {

        StringBuilder sb = new StringBuilder();
        sb.append("TASK: Implement the body of the following Java method.\n\n");

        if (packageName != null && !packageName.isBlank()) {
            sb.append("Package: ").append(packageName).append("\n");
        }

        if (imports != null && !imports.isEmpty()) {
            sb.append("Available Imports:\n");
            for (String imp : imports) {
                sb.append("  ").append(imp).append("\n");
            }
        }

        sb.append("\nTarget Class: ").append(className).append("\n");

        if (classFieldsAndExistingMethods != null && !classFieldsAndExistingMethods.isBlank()) {
            sb.append("Class Members & Sibling Methods (reuse these where appropriate):\n");
            sb.append(classFieldsAndExistingMethods).append("\n");
        }

        if (siblingContext != null && !siblingContext.isBlank()) {
            sb.append("\nInteracting Components / Sibling Classes in Project:\n");
            sb.append(siblingContext).append("\n");
        }

        sb.append("\nTarget Method Signature to Implement:\n");
        sb.append("  ").append(methodSignature).append("\n\n");

        sb.append("Requirement / Vibe Prompt:\n");
        sb.append("\"").append(prompt).append("\"\n\n");

        sb.append("Remember: Output ONLY the method block starting with '{' and ending with '}'. No markdown.");
        return sb.toString();
    }

    /**
     * Builds a prompt for generating class members (fields, constructors, methods).
     */
    public static String buildClassPrompt(
            String prompt,
            String packageName,
            List<String> imports,
            String classHeader,
            String siblingContext) {

        StringBuilder sb = new StringBuilder();
        sb.append("TASK: Implement the complete internal members (fields, constructors, methods) for the following Java class/interface/record.\n\n");

        if (packageName != null && !packageName.isBlank()) {
            sb.append("Package: ").append(packageName).append("\n");
        }

        if (imports != null && !imports.isEmpty()) {
            sb.append("Available Imports:\n");
            for (String imp : imports) {
                sb.append("  ").append(imp).append("\n");
            }
        }

        sb.append("\nClass Declaration:\n  ").append(classHeader).append("\n");

        if (siblingContext != null && !siblingContext.isBlank()) {
            sb.append("\nInteracting Components / Sibling Classes in Project:\n");
            sb.append(siblingContext).append("\n");
        }

        sb.append("\nRequirement / Vibe Prompt:\n");
        sb.append("\"").append(prompt).append("\"\n\n");

        sb.append("Remember: Output ONLY the Java member declarations (fields, constructors, methods). Do NOT repeat the class header or outer class braces.");
        return sb.toString();
    }

    /**
     * Builds a 1-shot self-healing correction prompt when the previous generation failed compiler parsing.
     */
    public static String buildSelfHealingMethodPrompt(
            String originalPrompt,
            String methodSig,
            String faultyCode,
            String errorMessage) {

        return "CRITICAL: The previous Java code you generated produced a compilation/syntax error!\n\n" +
                "Target Method Signature:\n  " + methodSig + "\n\n" +
                "Original Requirement:\n\"" + originalPrompt + "\"\n\n" +
                "Faulty Code:\n" + faultyCode + "\n\n" +
                "Compiler Error Message:\n" + errorMessage + "\n\n" +
                "TASK: Correct the syntax/compile error and output ONLY the valid Java method block starting with '{' and ending with '}'. No markdown.";
    }

    /**
     * Builds a 1-shot self-healing correction prompt for class members.
     */
    public static String buildSelfHealingClassPrompt(
            String originalPrompt,
            String classHeader,
            String faultyCode,
            String errorMessage) {

        return "CRITICAL: The previous Java class members you generated produced a compilation/syntax error!\n\n" +
                "Target Class Declaration:\n  " + classHeader + "\n\n" +
                "Original Requirement:\n\"" + originalPrompt + "\"\n\n" +
                "Faulty Code:\n" + faultyCode + "\n\n" +
                "Compiler Error Message:\n" + errorMessage + "\n\n" +
                "TASK: Correct the error and output ONLY the valid Java member declarations. No markdown.";
    }
}
