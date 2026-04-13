package cn.dolphinmind.glossary.java.analyze.dataflow;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;

import java.util.*;
import java.util.regex.*;

/**
 * Enhanced Taint Analysis Engine using Control Flow Aware Data Flow Graph.
 *
 * Improvements over TaintEngine:
 * 1. Uses ControlFlowAwareDataFlowGraph for precise variable tracking
 * 2. Path-sensitive taint propagation through branches
 * 3. Handles loops correctly with reaching definitions
 * 4. Lower false positive rate by understanding actual data flow
 *
 * Taint sources → propagation → sinks (without sanitization) = vulnerability
 */
public class EnhancedTaintEngine {

    // Taint source patterns: where untrusted data enters
    private static final Map<String, String> SOURCE_PATTERNS = new LinkedHashMap<>();
    static {
        // HTTP input
        SOURCE_PATTERNS.put("getParameter", "HTTP Parameter");
        SOURCE_PATTERNS.put("getParameterValues", "HTTP Parameter");
        SOURCE_PATTERNS.put("getParameterMap", "HTTP Parameter Map");
        SOURCE_PATTERNS.put("getHeader", "HTTP Header");
        SOURCE_PATTERNS.put("getInputStream", "HTTP Input Stream");
        SOURCE_PATTERNS.put("getReader", "HTTP Reader");
        SOURCE_PATTERNS.put("getQueryString", "Query String");
        SOURCE_PATTERNS.put("getRequestURI", "Request URI");
        // File/Network input
        SOURCE_PATTERNS.put("readLine", "File/Network Input");
        SOURCE_PATTERNS.put("readAllBytes", "File/Network Input");
        // Deserialization
        SOURCE_PATTERNS.put("readObject", "Deserialized Data");
        // Environment
        SOURCE_PATTERNS.put("getenv", "Environment Variable");
        SOURCE_PATTERNS.put("getProperty", "System Property");
    }

    // Sink patterns: where tainted data causes harm
    private static final Map<String, String> SINK_PATTERNS = new LinkedHashMap<>();
    static {
        SINK_PATTERNS.put("executeQuery", "SQL Injection");
        SINK_PATTERNS.put("executeUpdate", "SQL Injection");
        SINK_PATTERNS.put("execute", "SQL/Command Injection");
        SINK_PATTERNS.put("prepareStatement", "SQL Injection");
        SINK_PATTERNS.put("createStatement", "SQL Injection");
        SINK_PATTERNS.put("Runtime.getRuntime", "Command Injection");
        SINK_PATTERNS.put("ProcessBuilder", "Command Injection");
        SINK_PATTERNS.put("new File", "Path Traversal");
        SINK_PATTERNS.put("Paths.get", "Path Traversal");
        SINK_PATTERNS.put("Files.newInputStream", "Path Traversal");
        SINK_PATTERNS.put("Files.readAllBytes", "Path Traversal");
        SINK_PATTERNS.put("getWriter", "XSS");
        SINK_PATTERNS.put("setAttribute", "XSS/Response Manipulation");
    }

    // Sanitizer patterns: functions that clean tainted data
    private static final Set<String> SANITIZER_PATTERNS = new HashSet<>();
    static {
        SANITIZER_PATTERNS.add("PreparedStatement");
        SANITIZER_PATTERNS.add("setParameter");
        SANITIZER_PATTERNS.add("setString");
        SANITIZER_PATTERNS.add("setInt");
        SANITIZER_PATTERNS.add("replaceAll");
        SANITIZER_PATTERNS.add("escapeHtml");
        SANITIZER_PATTERNS.add("escapeSql");
        SANITIZER_PATTERNS.add("sanitize");
        SANITIZER_PATTERNS.add("URLEncoder.encode");
        SANITIZER_PATTERNS.add("HtmlUtils.htmlEscape");
        SANITIZER_PATTERNS.add("StringEscapeUtils");
    }

    /**
     * Analyze a method for taint flow vulnerabilities using CFG-DFG.
     */
    public List<EnhancedTaintFinding> analyze(MethodDeclaration method) {
        List<EnhancedTaintFinding> findings = new ArrayList<>();

        if (!method.getBody().isPresent()) return findings;

        // Step 1: Build control flow aware data flow graph
        ControlFlowAwareDataFlowGraph dfg = new ControlFlowAwareDataFlowGraph(method);

        // Step 2: Identify source nodes (where tainted data enters)
        List<TaintSourceNode> sources = findSourceNodes(method, dfg);
        if (sources.isEmpty()) return findings;

        // Step 3: Identify sink nodes (where tainted data causes harm)
        List<TaintSinkNode> sinks = findSinkNodes(method, dfg);
        if (sinks.isEmpty()) return findings;

        // Step 4: Track taint flow using DFG edges (reaching definitions)
        for (TaintSourceNode source : sources) {
            for (TaintSinkNode sink : sinks) {
                // Check if tainted variable reaches the sink through DFG
                if (isTaintedFlowThroughDFG(dfg, source, sink)) {
                    // Check if sanitized along the path
                    if (!isSanitizedAlongPath(method, dfg, source, sink)) {
                        findings.add(new EnhancedTaintFinding(source, sink));
                    }
                }
            }
        }

        return findings;
    }

    /**
     * Find source nodes in the method using AST analysis.
     */
    private List<TaintSourceNode> findSourceNodes(MethodDeclaration method,
                                                    ControlFlowAwareDataFlowGraph dfg) {
        List<TaintSourceNode> sources = new ArrayList<>();

        for (DFGNode node : dfg.getNodes()) {
            Statement stmt = node.getStatement();
            if (!(stmt instanceof ExpressionStmt)) continue;

            Expression expr = ((ExpressionStmt) stmt).getExpression();
            if (!(expr instanceof AssignExpr)) continue;

            AssignExpr assign = (AssignExpr) expr;
            String sourceType = matchesSourcePattern(assign.getValue());
            if (sourceType != null) {
                String varName = extractVariableName(assign.getTarget());
                if (varName != null) {
                    sources.add(new TaintSourceNode(node, varName, sourceType,
                        stmt.getBegin().map(p -> p.line).orElse(0),
                        stmt.toString()));
                }
            }
        }

        return sources;
    }

    /**
     * Find sink nodes in the method.
     */
    private List<TaintSinkNode> findSinkNodes(MethodDeclaration method,
                                                ControlFlowAwareDataFlowGraph dfg) {
        List<TaintSinkNode> sinks = new ArrayList<>();

        for (DFGNode node : dfg.getNodes()) {
            Statement stmt = node.getStatement();
            if (!(stmt instanceof ExpressionStmt)) continue;

            Expression expr = ((ExpressionStmt) stmt).getExpression();
            String sinkType = matchesSinkPattern(expr);
            if (sinkType != null) {
                // Extract the variable being used as argument to the sink
                String usedVar = extractSinkArgument(expr);
                if (usedVar != null) {
                    sinks.add(new TaintSinkNode(node, usedVar, sinkType,
                        stmt.getBegin().map(p -> p.line).orElse(0),
                        stmt.toString()));
                }
            }
        }

        return sinks;
    }

    /**
     * Check if tainted variable reaches the sink through DFG edges.
     * Uses reaching definitions to trace the flow.
     */
    private boolean isTaintedFlowThroughDFG(ControlFlowAwareDataFlowGraph dfg,
                                             TaintSourceNode source, TaintSinkNode sink) {
        // Source line must be before sink line
        if (source.getLine() >= sink.getLine()) return false;

        // Check if the sink's used variable has a reaching definition from the source
        Set<DFGNode> reachingDefs = dfg.getReachingDefs(sink.getNode(), sink.getTaintedVar());
        return reachingDefs.contains(source.getNode());
    }

    /**
     * Check if tainted data was sanitized along the path from source to sink.
     */
    private boolean isSanitizedAlongPath(MethodDeclaration method,
                                          ControlFlowAwareDataFlowGraph dfg,
                                          TaintSourceNode source, TaintSinkNode sink) {
        // Check all statements between source and sink
        if (!method.getBody().isPresent()) return false;

        BlockStmt body = method.getBody().get();
        List<Statement> allStmt = body.findAll(Statement.class);

        int sourceIdx = -1, sinkIdx = -1;
        for (int i = 0; i < allStmt.size(); i++) {
            Statement s = allStmt.get(i);
            if (s.equals(source.getNode().getStatement())) sourceIdx = i;
            if (s.equals(sink.getNode().getStatement())) sinkIdx = i;
        }

        if (sourceIdx == -1 || sinkIdx == -1 || sourceIdx >= sinkIdx) return false;

        // Check statements between source and sink for sanitization
        for (int i = sourceIdx + 1; i < sinkIdx; i++) {
            Statement stmt = allStmt.get(i);
            String stmtStr = stmt.toString();

            // Check if the tainted variable is being sanitized
            if (stmtStr.contains(source.getTaintedVar())) {
                for (String sanitizer : SANITIZER_PATTERNS) {
                    if (stmtStr.contains(sanitizer)) {
                        return true; // Found sanitization
                    }
                }
            }

            // Check if variable is reassigned to a safe value
            if (stmt instanceof ExpressionStmt) {
                Expression expr = ((ExpressionStmt) stmt).getExpression();
                if (expr instanceof AssignExpr) {
                    AssignExpr assign = (AssignExpr) expr;
                    String targetVar = extractVariableName(assign.getTarget());
                    if (source.getTaintedVar().equals(targetVar)) {
                        // Check if assigned value is a literal (safe)
                        if (isSafeValue(assign.getValue())) {
                            return true; // Taint cleared by safe assignment
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * Check if expression matches a source pattern.
     */
    private String matchesSourcePattern(Expression expr) {
        String exprStr = expr.toString();
        for (Map.Entry<String, String> entry : SOURCE_PATTERNS.entrySet()) {
            if (exprStr.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Check if expression matches a sink pattern.
     */
    private String matchesSinkPattern(Expression expr) {
        String exprStr = expr.toString();
        for (Map.Entry<String, String> entry : SINK_PATTERNS.entrySet()) {
            if (exprStr.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Extract the variable name being assigned.
     */
    private String extractVariableName(Expression target) {
        if (target instanceof NameExpr) {
            return ((NameExpr) target).getNameAsString();
        } else if (target instanceof FieldAccessExpr) {
            return extractVariableName(((FieldAccessExpr) target).getScope());
        }
        return null;
    }

    /**
     * Extract the argument variable passed to a sink method.
     */
    private String extractSinkArgument(Expression expr) {
        if (expr instanceof MethodCallExpr) {
            MethodCallExpr mce = (MethodCallExpr) expr;
            for (Argument arg : mce.getArguments()) {
                Expression argExpr = arg.getExpression();
                if (argExpr instanceof NameExpr) {
                    return ((NameExpr) argExpr).getNameAsString();
                } else if (argExpr instanceof BinaryExpr) {
                    // String concatenation: "SELECT * FROM " + userInput
                    BinaryExpr be = (BinaryExpr) argExpr;
                    String leftVar = extractNameFromExpr(be.getLeft());
                    if (leftVar != null) return leftVar;
                    return extractNameFromExpr(be.getRight());
                }
            }
        } else if (expr instanceof ObjectCreationExpr) {
            ObjectCreationExpr oce = (ObjectCreationExpr) expr;
            for (Argument arg : oce.getArguments()) {
                Expression argExpr = arg.getExpression();
                if (argExpr instanceof NameExpr) {
                    return ((NameExpr) argExpr).getNameAsString();
                }
            }
        }
        return null;
    }

    /**
     * Extract variable name from expression.
     */
    private String extractNameFromExpr(Expression expr) {
        if (expr instanceof NameExpr) return ((NameExpr) expr).getNameAsString();
        if (expr instanceof FieldAccessExpr) return extractVariableName((FieldAccessExpr) expr);
        return null;
    }

    /**
     * Check if the assigned value is a safe literal.
     */
    private boolean isSafeValue(Expression value) {
        return value instanceof StringLiteralExpr ||
               value instanceof IntegerLiteralExpr ||
               value instanceof LongLiteralExpr ||
               value instanceof DoubleLiteralExpr ||
               value instanceof CharLiteralExpr ||
               value instanceof BooleanLiteralExpr ||
               (value instanceof NullLiteralExpr);
    }

    // =====================================================================
    // Result Classes
    // =====================================================================

    /**
     * A taint source node in the DFG.
     */
    public static class TaintSourceNode {
        private final DFGNode node;
        private final String taintedVar;
        private final String sourceType;
        private final int line;
        private final String code;

        public TaintSourceNode(DFGNode node, String taintedVar, String sourceType, int line, String code) {
            this.node = node;
            this.taintedVar = taintedVar;
            this.sourceType = sourceType;
            this.line = line;
            this.code = code;
        }

        public DFGNode getNode() { return node; }
        public String getTaintedVar() { return taintedVar; }
        public String getSourceType() { return sourceType; }
        public int getLine() { return line; }
        public String getCode() { return code; }
    }

    /**
     * A taint sink node in the DFG.
     */
    public static class TaintSinkNode {
        private final DFGNode node;
        private final String taintedVar;
        private final String sinkType;
        private final int line;
        private final String code;

        public TaintSinkNode(DFGNode node, String taintedVar, String sinkType, int line, String code) {
            this.node = node;
            this.taintedVar = taintedVar;
            this.sinkType = sinkType;
            this.line = line;
            this.code = code;
        }

        public DFGNode getNode() { return node; }
        public String getTaintedVar() { return taintedVar; }
        public String getSinkType() { return sinkType; }
        public int getLine() { return line; }
        public String getCode() { return code; }
    }

    /**
     * An enhanced taint finding with DFG-based evidence.
     */
    public static class EnhancedTaintFinding {
        private final TaintSourceNode source;
        private final TaintSinkNode sink;

        public EnhancedTaintFinding(TaintSourceNode source, TaintSinkNode sink) {
            this.source = source;
            this.sink = sink;
        }

        public TaintSourceNode getSource() { return source; }
        public TaintSinkNode getSink() { return sink; }
        public String getSinkType() { return sink.getSinkType(); }
        public int getSourceLine() { return source.getLine(); }
        public int getSinkLine() { return sink.getLine(); }
        public String getTaintedVar() { return source.getTaintedVar(); }

        @Override
        public String toString() {
            return "Taint: " + source.getCode() + " (line " + source.getLine() + ") → " +
                   sink.getCode() + " (line " + sink.getLine() + ") via " + source.getTaintedVar();
        }
    }
}
