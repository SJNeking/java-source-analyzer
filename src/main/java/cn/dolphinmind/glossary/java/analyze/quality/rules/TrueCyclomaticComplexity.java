package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.cfg.CFGBuilder;
import cn.dolphinmind.glossary.java.analyze.cfg.CFGNode;
import cn.dolphinmind.glossary.java.analyze.cfg.ControlFlowGraph;
import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.QualityRule;
import cn.dolphinmind.glossary.java.analyze.quality.Severity;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.util.*;

/**
 * True cyclomatic complexity based on Control Flow Graph.
 * M = E - N + 2P (McCabe's formula)
 *
 * This is fundamentally different from the keyword-counting approach.
 * It builds the actual CFG and computes complexity from graph topology.
 */
public class TrueCyclomaticComplexity implements QualityRule {

    private final int threshold;

    public TrueCyclomaticComplexity(int threshold) {
        this.threshold = threshold;
    }

    public TrueCyclomaticComplexity() {
        this(15);
    }

    @Override
    public String getRuleKey() { return "RSPEC-3776-CFG"; }

    @Override
    public String getName() { return "Cyclomatic complexity should not exceed threshold (CFG-based)"; }

    @Override
    public String getCategory() { return "CODE_SMELL"; }

    @SuppressWarnings("unchecked")
    @Override
    public List<QualityIssue> check(Map<String, Object> classAsset) {
        List<QualityIssue> issues = new ArrayList<>();
        String className = (String) classAsset.getOrDefault("address", "");
        String filePath = (String) classAsset.getOrDefault("source_file", "");
        String sourceCode = (String) classAsset.getOrDefault("methods_full", "");

        List<Map<String, Object>> methods = (List<Map<String, Object>>) classAsset.getOrDefault("methods_full", Collections.emptyList());
        if (methods.isEmpty()) return issues;

        // Build CFG for the whole class's methods
        // Since we don't have raw source here, we analyze via the method bodies we have
        // For true CFG, we need the AST. We'll use the source_code field.

        for (Map<String, Object> method : methods) {
            String methodBody = (String) method.getOrDefault("body_code", "");
            String methodName = (String) method.get("name");
            String methodSig = (String) method.getOrDefault("address", methodName);
            int line = (int) method.getOrDefault("line_start", 0);

            if (methodBody == null || methodBody.isEmpty()) continue;

            // Try to build CFG from method body
            try {
                String fullMethod = "void dummy() {\n" + methodBody + "\n}";
                MethodDeclaration md = StaticJavaParser.parseMethodDeclaration(fullMethod);
                CFGBuilder builder = new CFGBuilder();
                ControlFlowGraph cfg = builder.build(md);

                int complexity = cfg.computeCyclomaticComplexity();
                if (complexity > threshold) {
                    issues.add(new QualityIssue(
                            getRuleKey(), getName(), Severity.MAJOR, getCategory(),
                            filePath, className, methodName, line,
                            "Cyclomatic complexity is " + complexity + " (threshold: " + threshold + ") [CFG-based]",
                            "cc=" + complexity + ", nodes=" + cfg.getNodes().size() + ", edges=" + cfg.getEdges().size()
                    ));
                }
            } catch (Exception e) {
                // If we can't parse the method body, skip
            }
        }

        return issues;
    }
}
