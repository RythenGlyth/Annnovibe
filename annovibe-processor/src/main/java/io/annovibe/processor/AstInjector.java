package io.annovibe.processor;

import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;

/**
 * Injects parsed AST trees directly into the javac compiler AST.
 */
public class AstInjector {

    private final ParserFactory parserFactory;
    private final TreeMaker treeMaker;

    public AstInjector(Context context) {
        this.parserFactory = ParserFactory.instance(context);
        this.treeMaker = TreeMaker.instance(context);
    }

    /**
     * Injects the parsed block into the method's AST body.
     */
    public void injectMethodBody(JCTree.JCMethodDecl methodDecl, String methodBodyCode) {
        String code = methodBodyCode.trim();
        if (!code.startsWith("{")) {
            code = "{\n" + code + "\n}";
        }
        var parser = parserFactory.newParser(code, false, false, false);
        JCTree.JCBlock block = parser.block();
        methodDecl.body = block;
    }

    /**
     * Injects parsed member declarations (fields, methods) into the class' AST.
     */
    public void injectClassMembers(JCTree.JCClassDecl classDecl, String classMembersCode) {
        String dummyClass = "class Dummy {\n" + classMembersCode + "\n}";
        var parser = parserFactory.newParser(dummyClass, false, false, false);
        JCTree.JCCompilationUnit cu = parser.parseCompilationUnit();
        if (cu.defs.nonEmpty() && cu.defs.head instanceof JCTree.JCClassDecl dummyDecl) {
            for (JCTree member : dummyDecl.defs) {
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
}
