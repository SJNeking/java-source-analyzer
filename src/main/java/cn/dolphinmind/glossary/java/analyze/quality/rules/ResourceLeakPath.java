package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.cfg.CFGBuilder;
import cn.dolphinmind.glossary.java.analyze.cfg.CFGNode;
import cn.dolphinmind.glossary.java.analyze.cfg.ControlFlowGraph;
import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.QualityRule;
import cn.dolphinmind.glossary.java.analyze.quality.Severity;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.Statement;

import java.util.*;
import java.util.regex.*;

/**
 * Detects potential resource leaks by analyzing all paths in the CFG.
 *
 * A resource leak occurs when:
 * 1. A resource is opened (new FileInputStream, new Socket, etc.)
 * 2. There exists a path to EXIT where the resource is never closed
 *
 * Uses path enumeration on the CFG (bounded depth).
 */
public class ResourceLeakPath implements QualityRule {

    // Resource open patterns
    private static final String[] OPEN_PATTERNS = {
        "new\\s+FileInputStream", "new\\s+FileOutputStream", "new\\s+BufferedReader",
        "new\\s+BufferedWriter", "new\\s+FileReader", "new\\s+FileWriter",
        "new\\s+Socket", "new\\s+ServerSocket", "new\\s+ObjectInputStream",
        "new\\s+ObjectOutputStream", "new\\s+PrintWriter", "new\\s+Scanner",
        "openConnection\\(", "createStatement\\(", "prepareStatement\\(",
        "Files\\.newInputStream", "Files\\.newOutputStream", "Files\\.newBufferedReader",
        "new\\s+RandomAccessFile", "DriverManager\\.getConnection"
    };

    // Close patterns
    private static final String[] CLOSE_PATTERNS = {
        "\\.close\\(", "\\.dispose\\(", "try\\s*\\(",
        "finally\\s*\\{", "\\.shutdown\\(", "\\.release\\("
    };

    private static final Pattern[] openRegex;
    private static final Pattern[] closeRegex;

    static {
        openRegex = new Pattern[OPEN_PATTERNS.length];
        for (int i = 0; i < OPEN_PATTERNS.length; i++) {
            openRegex[i] = Pattern.compile(OPEN_PATTERNS[i]);
        }
        closeRegex = new Pattern[CLOSE_PATTERNS.length];
        for (int i = 0; i < CLOSE_PATTERNS.length; i++) {
            closeRegex[i] = Pattern.compile(CLOSE_PATTERNS[i]);
        }
    }

    @Override
    public String getRuleKey() { return "RSPEC-2095-CFG"; }

    @Override
    public String getName() { return "Resources should be closed on all paths"; }

    @Override
    public String getCategory() { return "BUG"; }

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

            // Quick check: does method open any resource?
            boolean opensResource = false;
            for (Pattern pat : openRegex) {
                if (pat.matcher(methodBody).find()) {
                    opensResource = true;
                    break;
                }
            }
            if (!opensResource) continue;

            // Does it close?
            boolean hasClose = false;
            for (Pattern pat : closeRegex) {
                if (pat.matcher(methodBody).find()) {
                    hasClose = true;
                    break;
                }
            }

            // If it opens but doesn't have any close pattern at all
            if (!hasClose) {
                issues.add(new QualityIssue(
                        getRuleKey(), getName(), Severity.CRITICAL, getCategory(),
                        filePath, className, methodName, line,
                        "Resource opened but never closed (no close/try-with-resources/finally found)",
                        "open without close"
                ));
                continue;
            }

            // More precise check: build CFG and check if all paths from open to exit pass through close
            try {
                String fullMethod = "void dummy() {\n" + methodBody + "\n}";
                MethodDeclaration md = StaticJavaParser.parseMethodDeclaration(fullMethod);
                CFGBuilder builder = new CFGBuilder();
                ControlFlowGraph cfg = builder.build(md);

                // Find all paths from ENTRY to EXIT (bounded depth)
                List<List<CFGNode>> paths = cfg.findAllPaths(200);

                for (List<CFGNode> path : paths) {
                    boolean resourceOpened = false;
                    boolean resourceClosed = false;

                    for (CFGNode node : path) {
                        String nodeStr = node.toString();
                        for (Statement stmt : node.getStatements()) {
                            String stmtStr = stmt.toString();
                            for (Pattern pat : openRegex) {
                                if (pat.matcher(stmtStr).find()) {
                                    resourceOpened = true;
                                }
                            }
                            if (resourceOpened) {
                                for (Pattern pat : closeRegex) {
                                    if (pat.matcher(stmtStr).find()) {
                                        resourceClosed = true;
                                    }
                                }
                            }
                        }
                    }

                    if (resourceOpened && !resourceClosed) {
                        issues.add(new QualityIssue(
                                getRuleKey(), getName(), Severity.CRITICAL, getCategory(),
                                filePath, className, methodName, line,
                                "Resource leak: there exists a path where resource is opened but not closed (CFG path analysis)",
                                "open→exit without close"
                        ));
                        break; // One issue per method is enough
                    }
                }
            } catch (Exception e) {
                // Fallback: simple regex check already done above
            }
        }

        return issues;
    }
}
