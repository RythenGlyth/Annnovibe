package io.annovibe.processor;

import com.sun.source.util.Trees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import io.annovibe.AI;
import io.annovibe.llm.*;
import io.annovibe.vibe;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import java.util.*;

/**
 * Annotation processor for @AI and @vibe annotations.
 * Generates method bodies and class contents at compile-time via LLM and injects into javac AST.
 */
@SupportedAnnotationTypes({"io.annovibe.AI", "io.annovibe.vibe", "io.annovibe.annotations.Vibe"})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class AnnovibeProcessor extends AbstractProcessor {

    private Trees trees;
    private AstInjector astInjector;
    private SourceMaterializer sourceMaterializer;
    private boolean initialized = false;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        try {
            trees = Trees.instance(processingEnv);
            Context context = getJavacContext(processingEnv);
            if (context != null) {
                this.astInjector = new AstInjector(context);
                this.sourceMaterializer = new SourceMaterializer(trees);
                this.initialized = true;
            }
        } catch (Throwable t) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.WARNING,
                    "[Annovibe] Could not access javac internals: " + t.getMessage() + ". Please ensure compiler args include --add-exports."
            );
        }
    }

    private Context getJavacContext(ProcessingEnvironment env) {
        if (env instanceof JavacProcessingEnvironment jpe) {
            return jpe.getContext();
        }
        // Unwrap if decorated by Maven/Gradle wrapper
        try {
            var field = env.getClass().getDeclaredField("delegate");
            field.setAccessible(true);
            Object delegate = field.get(env);
            if (delegate instanceof JavacProcessingEnvironment jpe) {
                return jpe.getContext();
            }
        } catch (Exception ignored) {}
        return null;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver() || !initialized) {
            return false;
        }

        Set<Element> elements = new LinkedHashSet<>();
        elements.addAll(roundEnv.getElementsAnnotatedWith(AI.class));
        elements.addAll(roundEnv.getElementsAnnotatedWith(vibe.class));
        elements.addAll(roundEnv.getElementsAnnotatedWith(io.annovibe.annotations.Vibe.class));

        if (elements.isEmpty()) {
            return false;
        }

        ProjectContextGraph contextGraph = new ProjectContextGraph();
        contextGraph.analyze(elements, trees);

        boolean materializeOption = Boolean.parseBoolean(
                processingEnv.getOptions().getOrDefault("annovibe.materialize", "false")
        );

        List<Element> orderedTargets = contextGraph.getOrderedTargets();

        for (Element element : orderedTargets) {
            try {
                processElement(element, contextGraph, materializeOption);
            } catch (Exception e) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "[Annovibe] Failed to generate code for " + element.getSimpleName() + ": " + e.getMessage(),
                        element
                );
            }
        }

        return false;
    }

    private void processElement(Element element, ProjectContextGraph contextGraph, boolean materialize) throws Exception {
        VibeAnnotation vibe = VibeAnnotation.fromElement(element);
        if (vibe == null) return;

        LlmConfig config = LlmConfig.resolve(vibe.provider(), vibe.model(), vibe.temperature(), vibe.cache());
        CacheManager cache = new CacheManager(config.getCacheDir(), config.isCacheEnabled());

        String siblingContext = contextGraph.getSiblingContext(element);
        JCTree tree = (JCTree) trees.getTree(element);
        if (tree == null) return;

        if (element.getKind() == ElementKind.METHOD) {
            processMethod((ExecutableElement) element, (JCTree.JCMethodDecl) tree, vibe, config, cache, siblingContext, materialize);
        } else if (element.getKind().isClass() || element.getKind().isInterface()) {
            processClass((TypeElement) element, (JCTree.JCClassDecl) tree, vibe, config, cache, siblingContext);
        }
    }

    private void processMethod(
            ExecutableElement methodElem,
            JCTree.JCMethodDecl methodDecl,
            VibeAnnotation vibe,
            LlmConfig config,
            CacheManager cache,
            String siblingContext,
            boolean materialize) throws Exception {

        TypeElement enclosingClass = (TypeElement) methodElem.getEnclosingElement();
        String className = enclosingClass.getSimpleName().toString();
        String packageName = processingEnv.getElementUtils().getPackageOf(enclosingClass).getQualifiedName().toString();

        StringBuilder membersBuilder = new StringBuilder();
        for (Element m : enclosingClass.getEnclosedElements()) {
            if (m.getKind() == ElementKind.FIELD) {
                membersBuilder.append("    ").append(m.asType()).append(" ").append(m.getSimpleName()).append(";\n");
            } else if (m.getKind() == ElementKind.METHOD && !m.equals(methodElem)) {
                membersBuilder.append("    ").append(((ExecutableElement) m).getReturnType())
                        .append(" ").append(m.getSimpleName()).append("(...);\n");
            }
        }

        String methodSig = methodElem.getReturnType() + " " + methodElem.getSimpleName() + "(";
        List<? extends VariableElement> params = methodElem.getParameters();
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) methodSig += ", ";
            methodSig += params.get(i).asType() + " " + params.get(i).getSimpleName();
        }
        methodSig += ")";

        String hash = CacheManager.computeHash(
                config.getProvider().getKey(),
                config.getModel(),
                vibe.prompt(),
                methodSig,
                membersBuilder.toString(),
                siblingContext
        );

        String generatedCode;
        Optional<String> cached = cache.get(hash);
        if (cached.isPresent()) {
            generatedCode = cached.get();
        } else {
            String systemPrompt = PromptBuilder.buildSystemPrompt(vibe.systemPrompt(), config.getGlobalPrompt());
            String userPrompt = PromptBuilder.buildMethodPrompt(
                    vibe.prompt(),
                    packageName,
                    Collections.emptyList(),
                    className,
                    membersBuilder.toString(),
                    methodSig,
                    siblingContext
            );

            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.NOTE,
                    "[Annovibe] Generating body for " + className + "." + methodElem.getSimpleName() +
                            " using " + config.getProvider().getKey() + " (" + config.getModel() + ")..."
            );

            LlmClient client = LlmClientFactory.create(config);
            String rawResponse = client.complete(systemPrompt, userPrompt, vibe.temperature());
            generatedCode = CodeExtractor.normalizeMethodBody(rawResponse);
            cache.put(hash, generatedCode);
        }

        // Attempt AST injection with bounded compile-time self-healing loop
        int maxRetries = config.getMaxSelfHealingRetries();
        int attempt = 0;
        boolean injected = false;

        while (!injected) {
            try {
                astInjector.injectMethodBody(methodDecl, generatedCode);
                injected = true;
            } catch (Throwable parseError) {
                attempt++;
                if (attempt > maxRetries) {
                    throw new RuntimeException(
                            "Syntax error in generated code. Self-healing aborted after reaching max depth (" +
                                    maxRetries + " attempts). Last error: " + parseError.getMessage(),
                            parseError
                    );
                }

                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.WARNING,
                        "[Annovibe] ⚡ Syntax error in generated code (" + parseError.getMessage() +
                                "). Triggering self-healing attempt " + attempt + "/" + maxRetries + " for " +
                                className + "." + methodElem.getSimpleName() + "...",
                        methodElem
                );

                String fixPrompt = PromptBuilder.buildSelfHealingMethodPrompt(
                        vibe.prompt(),
                        methodSig,
                        generatedCode,
                        parseError.getMessage()
                );
                String systemPrompt = PromptBuilder.buildSystemPrompt(vibe.systemPrompt(), config.getGlobalPrompt());
                LlmClient client = LlmClientFactory.create(config);
                String healedResponse = client.complete(systemPrompt, fixPrompt, 0.1);
                generatedCode = CodeExtractor.normalizeMethodBody(healedResponse);
            }
        }

        if (attempt > 0) {
            cache.put(hash, generatedCode);
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.NOTE,
                    "[Annovibe] ✨ Self-healing succeeded on attempt " + attempt + "! Generated code corrected and compiled cleanly."
            );
        }

        // Optional source file materialization
        if (materialize) {
            sourceMaterializer.materializeMethod(methodElem, generatedCode);
        }
    }

    private void processClass(
            TypeElement typeElem,
            JCTree.JCClassDecl classDecl,
            VibeAnnotation vibe,
            LlmConfig config,
            CacheManager cache,
            String siblingContext) throws Exception {

        String className = typeElem.getSimpleName().toString();
        String packageName = processingEnv.getElementUtils().getPackageOf(typeElem).getQualifiedName().toString();

        String classHeader = typeElem.getKind().name().toLowerCase() + " " + className;

        String hash = CacheManager.computeHash(
                config.getProvider().getKey(),
                config.getModel(),
                vibe.prompt(),
                classHeader,
                siblingContext
        );

        String generatedCode;
        Optional<String> cached = cache.get(hash);
        if (cached.isPresent()) {
            generatedCode = cached.get();
        } else {
            String systemPrompt = PromptBuilder.buildSystemPrompt(vibe.systemPrompt(), config.getGlobalPrompt());
            String userPrompt = PromptBuilder.buildClassPrompt(
                    vibe.prompt(),
                    packageName,
                    Collections.emptyList(),
                    classHeader,
                    siblingContext
            );

            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.NOTE,
                    "[Annovibe] Generating members for class " + className +
                            " using " + config.getProvider().getKey() + " (" + config.getModel() + ")..."
            );

            LlmClient client = LlmClientFactory.create(config);
            String rawResponse = client.complete(systemPrompt, userPrompt, vibe.temperature());
            generatedCode = CodeExtractor.extractCode(rawResponse);
            cache.put(hash, generatedCode);
        }

        // Attempt class AST injection with bounded compile-time self-healing loop
        int maxRetries = config.getMaxSelfHealingRetries();
        int attempt = 0;
        boolean injected = false;

        while (!injected) {
            try {
                astInjector.injectClassMembers(classDecl, generatedCode);
                injected = true;
            } catch (Throwable parseError) {
                attempt++;
                if (attempt > maxRetries) {
                    throw new RuntimeException(
                            "Syntax error in generated class members. Self-healing aborted after reaching max depth (" +
                                    maxRetries + " attempts). Last error: " + parseError.getMessage(),
                            parseError
                    );
                }

                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.WARNING,
                        "[Annovibe] ⚡ Syntax error in generated class members (" + parseError.getMessage() +
                                "). Triggering self-healing attempt " + attempt + "/" + maxRetries + " for class " + className + "...",
                        typeElem
                );

                String fixPrompt = PromptBuilder.buildSelfHealingClassPrompt(
                        vibe.prompt(),
                        classHeader,
                        generatedCode,
                        parseError.getMessage()
                );
                String systemPrompt = PromptBuilder.buildSystemPrompt(vibe.systemPrompt(), config.getGlobalPrompt());
                LlmClient client = LlmClientFactory.create(config);
                String healedResponse = client.complete(systemPrompt, fixPrompt, 0.1);
                generatedCode = CodeExtractor.extractCode(healedResponse);
            }
        }

        if (attempt > 0) {
            cache.put(hash, generatedCode);
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.NOTE,
                    "[Annovibe] ✨ Self-healing succeeded on attempt " + attempt + "! Generated class members corrected and compiled cleanly."
            );
        }
    }
}
