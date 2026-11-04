package io.annovibe.processor;

import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;

import javax.lang.model.element.Element;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Materializes generated code directly back into .java source files on disk.
 */
public class SourceMaterializer {

    private final Trees trees;

    public SourceMaterializer(Trees trees) {
        this.trees = trees;
    }

    /**
     * Replaces the method body in the source file on disk.
     */
    public boolean materializeMethod(Element element, String newBodyCode) {
        try {
            TreePath path = trees.getPath(element);
            if (path == null) return false;

            JavaFileObject fileObject = path.getCompilationUnit().getSourceFile();
            if (fileObject == null || fileObject.getKind() != JavaFileObject.Kind.SOURCE) return false;

            URItoPath(fileObject);
            Path filePath = Paths.get(fileObject.toUri());
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) return false;

            String content = Files.readString(filePath, StandardCharsets.UTF_8);
            JCTree.JCMethodDecl methodTree = (JCTree.JCMethodDecl) trees.getTree(element);
            if (methodTree == null || methodTree.body == null) return false;

            int startPos = methodTree.body.getStartPosition();
            int endPos = methodTree.body.getPreferredPosition();
            // Using character indices
            int treeStart = methodTree.body.pos;
            if (treeStart < 0 || treeStart >= content.length()) return false;

            // Find matching opening and closing braces
            int openBrace = content.indexOf('{', treeStart);
            if (openBrace == -1) return false;

            int closeBrace = findClosingBrace(content, openBrace);
            if (closeBrace == -1) return false;

            String formattedBody = newBodyCode.trim();
            if (!formattedBody.startsWith("{")) {
                formattedBody = "{\n        " + formattedBody.replace("\n", "\n        ") + "\n    }";
            }

            String updated = content.substring(0, openBrace) + formattedBody + content.substring(closeBrace + 1);
            Files.writeString(filePath, updated, StandardCharsets.UTF_8);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private int findClosingBrace(String content, int openPos) {
        int depth = 0;
        for (int i = openPos; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private void URItoPath(JavaFileObject fileObject) {
        // Validation helper
    }
}
