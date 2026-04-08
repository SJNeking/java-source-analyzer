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
 * Detects unreachable code: statements that cannot be reached from the method entry.
 *
 * Uses CFG reachability analysis (BFS from ENTRY node).
 * Common causes:
 * - Code after return/throw/break
 * - Code after continue in a loop
 * - Dead branches after always-true/false conditions
 */
public class UnreachableCode implements QualityRule {

    @Override
    public String getRuleKey() { return "RSPEC-4838"; }

    @Override
    public String getName() { return "Unreachable code should be removed"; }

    @Override
    public String getCategory() { return "CODE_SMELL"; }

    @SuppressWarnings("unchecked")
    @Override
    public List<QualityIssue> check(Map<String, Object> classAsset) {
        List<QualityIssue> issues = new ArrayList<>();
        String className = (String) classAsset.getOrDefault("address", "");
        String filePath = (String) classAsset.getOrDefault("source_file", "");

        List<Map<String, Object>> methods = (List<Map<String, Object>>) classAsset.getOrDefault("methods_full", Collections.emptyList());

        for (Map<String, Object> method : methods) {
            String methodBody = (String) method.getOrDefault("body_code", "");
            String methodName = (String) method.get("name");
            int line = (int) method.getOrDefault("line_start", 0);

            if (methodBody == null || methodBody.isEmpty()) continue;

            try {
                String fullMethod = "void dummy() {\n" + methodBody + "\n}";
                MethodDeclaration md = StaticJavaParser.parseMethodDeclaration(fullMethod);
                CFGBuilder builder = new CFGBuilder();
                ControlFlowGraph cfg = builder.build(md);

                List<CFGNode> unreachable = cfg.findUnreachableNodes();
                if (!unreachable.isEmpty()) {
                    // Count only non-synthetic nodes
                    long realUnreachable = unreachable.stream()
                            .filter(n -> n.getType() != CFGNode.NodeType.ENTRY && n.getType() != CFGNode.NodeType.EXIT)
                            .count();

                    if (realUnreachable > 0) {
                        StringBuilder detail = new StringBuilder();
                        for (CFGNode node : unreachable) {
                            if (node.getType() != CFGNode.NodeType.ENTRY && node.getType() != CFGNode.NodeType.EXIT) {
                                detail.append(node.toString()).append("; ");
                            }
                        }
                        issues.add(new QualityIssue(
                                getRuleKey(), getName(), Severity.MAJOR, getCategory(),
                                filePath, className, methodName, line,
                                "Method contains " + realUnreachable + " unreachable code block(s)",
                                detail.toString()
                        ));
                    }
                }
            } catch (Exception e) {
                // If we can't parse, skip
            }
        }

        return issues;
    }
}
