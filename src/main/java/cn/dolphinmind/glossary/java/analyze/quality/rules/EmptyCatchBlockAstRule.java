package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.Severity;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.*;

/**
 * RSPEC-108: Empty catch blocks should be avoided.
 *
 * AST-based implementation using JavaParser instead of regex.
 * Accurately detects catch blocks with truly empty bodies (not comments).
 */
public class EmptyCatchBlockAstRule extends AstBasedMethodRule {

    @Override
    public String getRuleKey() { return "RSPEC-108"; }
    @Override
    public String getName() { return "Empty catch blocks should be avoided"; }
    @Override
    public String getCategory() { return "BUG"; }

    @Override
    protected void visitAst(CompilationUnit cu, String methodName, String filePath,
                             String className, int lineStart, List<QualityIssue> issues) {
        cu.accept(new EmptyCatchVisitor(methodName, filePath, className, lineStart, issues), null);
    }

    private static class EmptyCatchVisitor extends VoidVisitorAdapter<Void> {
        private final String methodName;
        private final String filePath;
        private final String className;
        private final int lineStart;
        private final List<QualityIssue> issues;

        EmptyCatchVisitor(String methodName, String filePath, String className,
                          int lineStart, List<QualityIssue> issues) {
            this.methodName = methodName;
            this.filePath = filePath;
            this.className = className;
            this.lineStart = lineStart;
            this.issues = issues;
        }

        @Override
        public void visit(CatchClause n, Void arg) {
            // Check if catch block body is truly empty (no statements, no comments)
            if (n.getBody().getStatements().isEmpty()) {
                issues.add(new QualityIssue.Builder()
                    .ruleKey("RSPEC-108")
                    .ruleName("Empty catch blocks should be avoided")
                    .severity(Severity.MAJOR)
                    .category("BUG")
                    .filePath(filePath)
                    .className(className)
                    .methodName(methodName)
                    .location(
                        n.getBegin().map(p -> p.line).orElse(lineStart),
                        n.getEnd().map(p -> p.line).orElse(lineStart),
                        n.getBegin().map(p -> p.column).orElse(1),
                        n.getEnd().map(p -> p.column).orElse(0)
                    )
                    .message("Empty catch block - swallowed exception")
                    .evidence("catch (...) {}")
                    .build());
            }
            super.visit(n, arg);
        }
    }
}
