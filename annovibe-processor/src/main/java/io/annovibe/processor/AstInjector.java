package io.annovibe.processor;

import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;

/**
 * Injects parsed AST trees directly into the javac compiler AST.
 */
public class AstInjector {

    private final Context context;
    private final ParserFactory parserFactory;
    private final TreeMaker treeMaker;

    public AstInjector(Context context) {
        this.context = context;
        this.parserFactory = ParserFactory.instance(context);
        this.treeMaker = TreeMaker.instance(context);
    }

    /**
     * Parses and validates a method body code string into a JCBlock.
     * Intercepts syntax errors via DeferredDiagnosticHandler to enable self-healing without polluting javac log.
     */
    public JCTree.JCBlock parseMethodBlock(String methodBodyCode) {
        String code = methodBodyCode.trim();
        if (!code.startsWith("{")) {
            code = "{\n" + code + "\n}";
        }

        com.sun.tools.javac.util.Log log = com.sun.tools.javac.util.Log.instance(context);
        com.sun.tools.javac.util.Log.DeferredDiagnosticHandler handler = log.new DeferredDiagnosticHandler();
        int oldErrors = log.nerrors;

        try {
            var parser = parserFactory.newParser(code, false, false, false);
            JCTree.JCBlock block = parser.block();

            if (!handler.getDiagnostics().isEmpty()) {
                StringBuilder errorMsg = new StringBuilder();
                for (var d : handler.getDiagnostics()) {
                    errorMsg.append(d.getMessage(java.util.Locale.ENGLISH)).append("; ");
                }
                log.popDiagnosticHandler(handler);
                log.nerrors = oldErrors;
                throw new IllegalArgumentException("Syntax error: " + errorMsg);
            }

            log.popDiagnosticHandler(handler);
            if (block == null) {
                throw new IllegalArgumentException("Syntax error: failed to parse method block");
            }
            return block;
        } catch (Throwable t) {
            log.popDiagnosticHandler(handler);
            log.nerrors = oldErrors;
            if (t instanceof IllegalArgumentException iae) throw iae;
            throw new IllegalArgumentException("Syntax error: " + t.getMessage(), t);
        }
    }

    /**
     * Injects the parsed block into the method's AST body.
     */
    public void injectMethodBody(JCTree.JCMethodDecl methodDecl, String methodBodyCode) {
        methodDecl.body = parseMethodBlock(methodBodyCode);
    }

    /**
     * Parses and validates class members.
     */
    public java.util.List<JCTree> parseClassMembers(String classMembersCode) {
        String dummyClass = "class Dummy {\n" + classMembersCode + "\n}";
        com.sun.tools.javac.util.Log log = com.sun.tools.javac.util.Log.instance(context);
        com.sun.tools.javac.util.Log.DeferredDiagnosticHandler handler = log.new DeferredDiagnosticHandler();
        int oldErrors = log.nerrors;

        try {
            var parser = parserFactory.newParser(dummyClass, false, false, false);
            JCTree.JCCompilationUnit cu = parser.parseCompilationUnit();

            if (!handler.getDiagnostics().isEmpty()) {
                StringBuilder errorMsg = new StringBuilder();
                for (var d : handler.getDiagnostics()) {
                    errorMsg.append(d.getMessage(java.util.Locale.ENGLISH)).append("; ");
                }
                log.popDiagnosticHandler(handler);
                log.nerrors = oldErrors;
                throw new IllegalArgumentException("Syntax error: " + errorMsg);
            }

            log.popDiagnosticHandler(handler);

            if (cu == null || cu.defs.isEmpty() || !(cu.defs.head instanceof JCTree.JCClassDecl dummyDecl)) {
                throw new IllegalArgumentException("Syntax error: failed to parse class members");
            }

            java.util.List<JCTree> members = new java.util.ArrayList<>();
            for (JCTree member : dummyDecl.defs) {
                members.add(member);
            }
            return members;
        } catch (Throwable t) {
            log.popDiagnosticHandler(handler);
            log.nerrors = oldErrors;
            if (t instanceof IllegalArgumentException iae) throw iae;
            throw new IllegalArgumentException("Syntax error: " + t.getMessage(), t);
        }
    }

    /**
     * Injects parsed member declarations (fields, methods) into the class' AST.
     */
    public void injectClassMembers(JCTree.JCClassDecl classDecl, String classMembersCode) {
        java.util.List<JCTree> parsedMembers = parseClassMembers(classMembersCode);
        for (JCTree member : parsedMembers) {
            // Filter out default constructor if classDecl already has one
            if (member instanceof JCTree.JCMethodDecl md && md.name.toString().equals("<init>")) {
                boolean alreadyHasInit = false;
                for (JCTree existing : classDecl.defs) {
                    if (existing instanceof JCTree.JCMethodDecl em && em.name.toString().equals("<init>")) {
                        alreadyHasInit = true;
                        break;
                    }
                }
                if (alreadyHasInit) continue;
            }
            classDecl.defs = classDecl.defs.append(member);
        }
    }
}
