package io.annovibe.processor;

import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;

import javax.lang.model.element.*;
import java.util.*;

/**
 * Builds a holistic graph of classes and methods in the compilation round.
 * Enables sibling context injection so multiple @AI / @vibe annotations harmonize.
 */
public class ProjectContextGraph {

    private final Map<String, String> classSummaries = new LinkedHashMap<>();
    private final List<Element> targetElements = new ArrayList<>();

    public void analyze(Set<? extends Element> elements, Trees trees) {
        for (Element elem : elements) {
            targetElements.add(elem);
            Element enclosing = elem.getKind().isClass() || elem.getKind().isInterface() ? elem : elem.getEnclosingElement();
            if (enclosing != null && (enclosing.getKind().isClass() || enclosing.getKind().isInterface())) {
                TypeElement typeElem = (TypeElement) enclosing;
                String qName = typeElem.getQualifiedName().toString();
                if (!classSummaries.containsKey(qName)) {
                    classSummaries.put(qName, summarizeType(typeElem));
                }
            }
        }
    }

    private String summarizeType(TypeElement type) {
        StringBuilder sb = new StringBuilder();
        sb.append(type.getKind().name().toLowerCase()).append(" ").append(type.getSimpleName()).append(" {\n");
        for (Element enclosed : type.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.FIELD) {
                VariableElement field = (VariableElement) enclosed;
                sb.append("    ").append(field.asType()).append(" ").append(field.getSimpleName()).append(";\n");
            } else if (enclosed.getKind() == ElementKind.METHOD) {
                ExecutableElement method = (ExecutableElement) enclosed;
                sb.append("    ").append(method.getReturnType()).append(" ").append(method.getSimpleName()).append("(");
                List<? extends VariableElement> params = method.getParameters();
                for (int i = 0; i < params.size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(params.get(i).asType()).append(" ").append(params.get(i).getSimpleName());
                }
                sb.append(");\n");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Produces sibling context for a specific element, omitting itself.
     */
    public String getSiblingContext(Element current) {
        Element enclosing = current.getKind().isClass() || current.getKind().isInterface() ? current : current.getEnclosingElement();
        String currentQName = (enclosing instanceof TypeElement te) ? te.getQualifiedName().toString() : "";

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : classSummaries.entrySet()) {
            if (!entry.getKey().equals(currentQName)) {
                sb.append("// Sibling component: ").append(entry.getKey()).append("\n");
                sb.append(entry.getValue()).append("\n\n");
            }
        }
        return sb.toString().trim();
    }

    /**
     * Sorts targets topologically (classes/records first, then methods).
     */
    public List<Element> getOrderedTargets() {
        List<Element> sorted = new ArrayList<>(targetElements);
        sorted.sort((e1, e2) -> {
            boolean isType1 = e1.getKind().isClass() || e1.getKind().isInterface();
            boolean isType2 = e2.getKind().isClass() || e2.getKind().isInterface();
            if (isType1 && !isType2) return -1;
            if (!isType1 && isType2) return 1;
            return 0;
        });
        return sorted;
    }
}
