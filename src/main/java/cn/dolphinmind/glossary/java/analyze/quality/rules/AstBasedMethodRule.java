package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.Severity;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.*;

/**
 * AST-based method rule example using JavaParser instead of regex.
 *
 * This demonstrates the improved approach:
 * - Real AST traversal instead of string matching
 * - Precise line/column location
 * - Actual code snippet extraction
 * - Lower false positive rate
 */
public abstract class AstBasedMethodRule extends AbstractMethodRule {

    /**
     * Parse the method body_code into a CompilationUnit and run AST analysis.
     */
    @Override
    protected List<QualityIssue> checkMethod(Map<String, Object> method, String filePath, String className) {
        String methodName = (String) method.getOrDefault("name", "");
        String bodyCode = (String) method.getOrDefault("body_code", "");
        int lineStart = (int) method.getOrDefault("line_start", 0);

        if (bodyCode == null || bodyCode.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            // Wrap method body in a dummy method to parse
            String wrappedCode = wrapMethodBody(bodyCode);
            CompilationUnit cu = StaticJavaParser.parse(wrappedCode);

            List<QualityIssue> issues = new ArrayList<>();
            visitAst(cu, methodName, filePath, className, lineStart, issues);
            return issues;
        } catch (Exception e) {
            // If parsing fails, fall back to simple analysis
            logger.fine("AST parsing failed for method " + methodName + ", using fallback");
            return checkMethodFallback(method, filePath, className);
        }
    }

    /**
     * Wrap raw method body into a parseable method declaration.
     */
    private String wrapMethodBody(String bodyCode) {
        if (bodyCode.trim().startsWith("{")) {
            return "class Dummy { void dummy() " + bodyCode + "}";
        }
        return "class Dummy { void dummy() {\n" + bodyCode + "\n}}";
    }

    /**
     * Subclasses override this to visit AST nodes.
     */
    protected abstract void visitAst(CompilationUnit cu, String methodName,
                                      String filePath, String className, int lineStart,
                                      List<QualityIssue> issues);

    /**
     * Fallback for when AST parsing fails (e.g., incomplete code snippets).
     * Subclasses may override; default returns empty.
     */
    protected List<QualityIssue> checkMethodFallback(Map<String, Object> method,
                                                       String filePath, String className) {
        return Collections.emptyList();
    }

    /**
     * Helper: extract the actual code line(s) from the source.
     */
    protected String extractCodeSnippet(String bodyCode, int relativeLine) {
        String[] lines = bodyCode.split("\n");
        if (relativeLine >= 0 && relativeLine < lines.length) {
            return lines[relativeLine].trim();
        }
        return bodyCode.split("\n")[0].trim();
    }
}
