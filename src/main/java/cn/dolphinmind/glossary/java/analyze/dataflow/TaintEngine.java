package cn.dolphinmind.glossary.java.analyze.dataflow;

import com.github.javaparser.ast.body.MethodDeclaration;

import java.util.*;
import java.util.regex.*;

/**
 * Taint Analysis Engine: tracks tainted data from sources to sinks.
 *
 * A source is a point where untrusted data enters (user input, network, file, etc.)
 * A sink is a point where data is used in a security-sensitive operation
 *   (SQL query, file path, HTTP response, command execution, etc.)
 * A sanitizer is a function that cleanses tainted data.
 *
 * If tainted data reaches a sink without being sanitized, it's a vulnerability.
 */
public class TaintEngine {

    /** Source patterns: where untrusted data originates */
    private static final String[] SOURCE_PATTERNS = {
        // HTTP input
        "getParameter", "getParameterValues", "getParameterMap",
        "getHeader", "getHeaders", "getHeaderNames",
        "getInputStream", "getReader",
        "getQueryString", "getRequestURI", "getRequestURL",
        // File/Network input
        "readLine", "read", "readAllBytes",
        "recv", "receive",
        // Deserialization
        "readObject",
        // Command line
        "args\\[", "getArgs",
        // Environment
        "getenv", "getProperty"
    };

    /** Sink patterns: where tainted data causes harm */
    private static final String[] SINK_PATTERNS = {
        // SQL injection
        "executeQuery", "executeUpdate", "execute", "prepareStatement",
        "createStatement", "nativeQuery", "createQuery",
        // Command injection
        "exec", "Runtime.getRuntime", "ProcessBuilder",
        // Path traversal
        "new File", "new FileInputStream", "new FileOutputStream",
        "new FileReader", "new FileWriter", "Paths.get", "Files.newInputStream",
        "Files.newOutputStream", "Files.readAllBytes",
        // XSS
        "print", "println", "write", "append",
        "setAttribute.*response", "getWriter",
        // LDAP injection
        "search", "lookup",
        // XPath injection
        "evaluate", "compile",
        // SSRF
        "openConnection", "openStream",
        // Deserialization
        "readObject",
        // Logging (log injection)
        "log\\.", "LOG\\.", "logger\\."
    };

    /** Sanitizer patterns: functions that clean tainted data */
    private static final String[] SANITIZER_PATTERNS = {
        "replaceAll", "replace", "escapeHtml", "escapeXml", "escapeJava",
        "escapeSql", "sanitize", "clean", "validate",
        "PreparedStatement", "setParameter", "setString", "setInt",
        "URLEncoder.encode", "HtmlUtils.htmlEscape",
        "StringEscapeUtils", "Validator",
        "Pattern.compile", "Matcher"
    };

    private static final Pattern[] sourceRegex;
    private static final Pattern[] sinkRegex;
    private static final Pattern[] sanitizerRegex;

    static {
        sourceRegex = compilePatterns(SOURCE_PATTERNS);
        sinkRegex = compilePatterns(SINK_PATTERNS);
        sanitizerRegex = compilePatterns(SANITIZER_PATTERNS);
    }

    private static Pattern[] compilePatterns(String[] patterns) {
        Pattern[] result = new Pattern[patterns.length];
        for (int i = 0; i < patterns.length; i++) {
            result[i] = Pattern.compile(patterns[i]);
        }
        return result;
    }

    /**
     * Analyze a method for taint flow vulnerabilities.
     * Returns a list of findings: source → ... → sink without sanitization.
     */
    public List<TaintFinding> analyze(MethodDeclaration method) {
        List<TaintFinding> findings = new ArrayList<>();

        if (!method.getBody().isPresent()) return findings;

        String methodBody = method.getBody().get().toString();
        String methodName = method.getNameAsString();
        int methodLine = method.getBegin().map(p -> p.line).orElse(0);

        // Step 1: Identify source statements (where tainted data enters)
        List<TaintSource> sources = findSources(methodBody, methodName, methodLine);
        if (sources.isEmpty()) return findings;

        // Step 2: Identify sink statements
        List<TaintSink> sinks = findSinks(methodBody, methodName, methodLine);
        if (sinks.isEmpty()) return findings;

        // Step 3: Identify sanitizer statements
        Set<String> sanitizedVars = findSanitizers(methodBody);

        // Step 4: Track taint flow using simple variable tracking
        // For each source, track the variable it taints
        // For each sink, check if the variable used was sanitized
        for (TaintSource source : sources) {
            String taintedVar = source.getVariable();
            if (taintedVar == null) continue;

            for (TaintSink sink : sinks) {
                String sinkVar = sink.getVariable();
                if (sinkVar == null) continue;

                // Check if the tainted variable reaches this sink
                if (isTaintedFlow(methodBody, taintedVar, sinkVar, source.getLine(), sink.getLine())) {
                    // Check if it was sanitized along the way
                    if (!isSanitized(methodBody, taintedVar, source.getLine(), sink.getLine(), sanitizedVars)) {
                        findings.add(new TaintFinding(
                                source, sink, methodName, methodLine
                        ));
                    }
                }
            }
        }

        return findings;
    }

    /**
     * Find all source points in the method body.
     */
    private List<TaintSource> findSources(String body, String methodName, int methodLine) {
        List<TaintSource> sources = new ArrayList<>();
        String[] lines = body.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            for (Pattern pat : sourceRegex) {
                if (pat.matcher(line).find()) {
                    // Extract the variable being assigned
                    String var = extractAssignedVariable(line);
                    sources.add(new TaintSource(methodName, methodLine + i + 1, line, pat.pattern(), var));
                    break;
                }
            }
        }
        return sources;
    }

    /**
     * Find all sink points in the method body.
     */
    private List<TaintSink> findSinks(String body, String methodName, int methodLine) {
        List<TaintSink> sinks = new ArrayList<>();
        String[] lines = body.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            for (Pattern pat : sinkRegex) {
                if (pat.matcher(line).find()) {
                    String var = extractUsedVariable(line);
                    sinks.add(new TaintSink(methodName, methodLine + i + 1, line, pat.pattern(), var));
                    break;
                }
            }
        }
        return sinks;
    }

    /**
     * Find all sanitizer calls.
     */
    private Set<String> findSanitizers(String body) {
        Set<String> sanitized = new HashSet<>();
        String[] lines = body.split("\n");
        for (String line : lines) {
            for (Pattern pat : sanitizerRegex) {
                if (pat.matcher(line).find()) {
                    // Extract what's being sanitized
                    String var = extractAssignedVariable(line);
                    if (var != null) sanitized.add(var);
                    // Also check method argument being sanitized
                    var = extractMethodArgument(line);
                    if (var != null) sanitized.add(var);
                }
            }
        }
        return sanitized;
    }

    /**
     * Simple check: does the tainted variable appear between source and sink?
     * This is a simplified version of reaching definitions analysis.
     */
    private boolean isTaintedFlow(String body, String taintedVar, String sinkVar, int sourceLine, int sinkLine) {
        String[] lines = body.split("\n");
        // Check if the tainted variable is used in or before the sink line
        for (int i = 0; i < Math.min(sinkLine, lines.length); i++) {
            if (lines[i].contains(taintedVar) || (sinkVar != null && lines[i].contains(sinkVar))) {
                return true;
            }
        }
        // Also check if same variable is referenced
        return body.contains(taintedVar);
    }

    /**
     * Check if the tainted variable was sanitized between source and sink.
     */
    private boolean isSanitized(String body, String taintedVar, int sourceLine, int sinkLine, Set<String> sanitizedVars) {
        String[] lines = body.split("\n");
        int startIdx = Math.max(0, sourceLine - 1);
        int endIdx = Math.min(lines.length, sinkLine);

        for (int i = startIdx; i < endIdx; i++) {
            String line = lines[i];
            // Check if any sanitizer is applied to the tainted variable
            if (line.contains(taintedVar)) {
                for (Pattern pat : sanitizerRegex) {
                    if (pat.matcher(line).find()) {
                        return true;
                    }
                }
            }
            // Check if the variable was reassigned from a safe source
            if (Pattern.compile(taintedVar + "\\s*=\\s*\"[^\"]*\"").matcher(line).find()) {
                return true; // Reassigned to a literal (safe)
            }
        }
        return false;
    }

    /**
     * Extract the variable being assigned in a statement.
     * e.g., "String name = request.getParameter("id");" → "name"
     */
    private String extractAssignedVariable(String line) {
        Matcher m = Pattern.compile("(\\w+)\\s*=").matcher(line);
        if (m.find()) return m.group(1);
        return null;
    }

    /**
     * Extract the variable being used as an argument.
     * e.g., "stmt.executeQuery(sql)" → "sql"
     */
    private String extractUsedVariable(String line) {
        // Look for method argument that is a variable
        Matcher m = Pattern.compile("\\((\\w+)\\)").matcher(line);
        while (m.find()) {
            String arg = m.group(1);
            if (!arg.equals("null") && !arg.matches("\\d+") && !arg.matches("\".*\"") &&
                !arg.equals("true") && !arg.equals("false")) {
                return arg;
            }
        }
        // Also check string concatenation
        m = Pattern.compile("\\+\\s*(\\w+)").matcher(line);
        if (m.find()) return m.group(1);
        return null;
    }

    /**
     * Extract method argument being sanitized.
     */
    private String extractMethodArgument(String line) {
        Matcher m = Pattern.compile("\\w+\\.\\w+\\s*\\(\\s*(\\w+)\\s*\\)").matcher(line);
        if (m.find()) {
            String arg = m.group(1);
            if (!arg.equals("null") && !arg.matches("\\d+")) return arg;
        }
        return null;
    }

    /**
     * A taint source: where untrusted data enters the program.
     */
    public static class TaintSource {
        private final String methodName;
        private final int line;
        private final String code;
        private final String pattern;
        private final String variable;

        public TaintSource(String methodName, int line, String code, String pattern, String variable) {
            this.methodName = methodName;
            this.line = line;
            this.code = code;
            this.pattern = pattern;
            this.variable = variable;
        }

        public String getMethodName() { return methodName; }
        public int getLine() { return line; }
        public String getCode() { return code; }
        public String getPattern() { return pattern; }
        public String getVariable() { return variable; }
    }

    /**
     * A taint sink: where tainted data reaches a security-sensitive operation.
     */
    public static class TaintSink {
        private final String methodName;
        private final int line;
        private final String code;
        private final String pattern;
        private final String variable;

        public TaintSink(String methodName, int line, String code, String pattern, String variable) {
            this.methodName = methodName;
            this.line = line;
            this.code = code;
            this.pattern = pattern;
            this.variable = variable;
        }

        public String getMethodName() { return methodName; }
        public int getLine() { return line; }
        public String getCode() { return code; }
        public String getPattern() { return pattern; }
        public String getVariable() { return variable; }
    }

    /**
     * A taint finding: a source-sink pair without sanitization.
     */
    public static class TaintFinding {
        private final TaintSource source;
        private final TaintSink sink;
        private final String methodName;
        private final int methodLine;

        public TaintFinding(TaintSource source, TaintSink sink, String methodName, int methodLine) {
            this.source = source;
            this.sink = sink;
            this.methodName = methodName;
            this.methodLine = methodLine;
        }

        public TaintSource getSource() { return source; }
        public TaintSink getSink() { return sink; }
        public String getMethodName() { return methodName; }
        public int getMethodLine() { return methodLine; }

        @Override
        public String toString() {
            return "Taint: " + source.getCode() + " (line " + source.getLine() + ") → " +
                   sink.getCode() + " (line " + sink.getLine() + ")";
        }
    }
}
