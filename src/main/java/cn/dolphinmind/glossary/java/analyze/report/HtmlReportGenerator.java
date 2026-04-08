package cn.dolphinmind.glossary.java.analyze.report;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates an interactive HTML Dashboard report from scan results.
 * This is the customer-facing deliverable - the visual interface that
 * makes the analysis actionable for architects, developers, and managers.
 */
public class HtmlReportGenerator {

    private final Map<String, Object> scanResult;
    private final String frameworkName;
    private final String version;
    private final String scanDate;
    private final String outputPath;

    public HtmlReportGenerator(Map<String, Object> scanResult, String outputPath) {
        this.scanResult = scanResult;
        this.frameworkName = (String) scanResult.getOrDefault("framework", "Unknown");
        this.version = (String) scanResult.getOrDefault("version", "unknown");
        this.scanDate = (String) scanResult.getOrDefault("scan_date", "unknown");
        this.outputPath = outputPath;
    }

    /**
     * Generate the complete HTML dashboard.
     */
    public void generate() throws IOException {
        String html = buildHtml();
        Files.write(Paths.get(outputPath), html.getBytes(StandardCharsets.UTF_8));
    }

    private String buildHtml() {
        Map<String, Object> qualitySummary = (Map<String, Object>) scanResult.getOrDefault("quality_summary", Collections.emptyMap());
        List<Map<String, Object>> qualityIssues = (List<Map<String, Object>>) scanResult.getOrDefault("quality_issues", Collections.emptyList());
        Map<String, Object> commentCoverage = (Map<String, Object>) scanResult.getOrDefault("comment_coverage", Collections.emptyMap());
        Map<String, Object> projectType = (Map<String, Object>) scanResult.getOrDefault("project_type", Collections.emptyMap());

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"zh-CN\">\n<head>\n");
        sb.append("<meta charset=\"UTF-8\">\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        sb.append("<title>").append(frameworkName).append(" v").append(version).append(" - 代码质量报告</title>\n");
        sb.append("<style>\n").append(getStyles()).append("\n</style>\n");
        sb.append("</head>\n<body>\n");

        // Header
        sb.append("<div class=\"header\">\n");
        sb.append("<h1>").append(frameworkName).append("</h1>\n");
        sb.append("<div class=\"subtitle\">Version: ").append(version).append(" | Scanned: ").append(scanDate).append("</div>\n");
        sb.append("</div>\n");

        // Summary Cards
        int totalIssues = qualityIssues.size();
        int critical = qualitySummary.containsKey("by_severity") ?
                ((Map<String, Number>) qualitySummary.get("by_severity")).getOrDefault("CRITICAL", 0).intValue() : 0;
        int major = qualitySummary.containsKey("by_severity") ?
                ((Map<String, Number>) qualitySummary.get("by_severity")).getOrDefault("MAJOR", 0).intValue() : 0;
        int minor = qualitySummary.containsKey("by_severity") ?
                ((Map<String, Number>) qualitySummary.get("by_severity")).getOrDefault("MINOR", 0).intValue() : 0;
        int bugCount = qualitySummary.containsKey("by_category") ?
                ((Map<String, Number>) qualitySummary.get("by_category")).getOrDefault("BUG", 0).intValue() : 0;
        int codeSmellCount = qualitySummary.containsKey("by_category") ?
                ((Map<String, Number>) qualitySummary.get("by_category")).getOrDefault("CODE_SMELL", 0).intValue() : 0;
        int securityCount = qualitySummary.containsKey("by_category") ?
                ((Map<String, Number>) qualitySummary.get("by_category")).getOrDefault("SECURITY", 0).intValue() : 0;

        double classCoverage = ((Number) commentCoverage.getOrDefault("class_comment_coverage_pct", 0)).doubleValue();
        double methodCoverage = ((Number) commentCoverage.getOrDefault("method_comment_coverage_pct", 0)).doubleValue();
        double fieldCoverage = ((Number) commentCoverage.getOrDefault("field_comment_coverage_pct", 0)).doubleValue();

        sb.append("<div class=\"container\">\n");
        sb.append("<div class=\"cards\">\n");
        sb.append(buildCard("Total Issues", String.valueOf(totalIssues), totalIssues > 0 ? "red" : "green"));
        sb.append(buildCard("Critical", String.valueOf(critical), critical > 0 ? "red" : "green"));
        sb.append(buildCard("Major", String.valueOf(major), major > 0 ? "orange" : "green"));
        sb.append(buildCard("Minor", String.valueOf(minor), "blue"));
        sb.append(buildCard("Bugs", String.valueOf(bugCount), bugCount > 0 ? "red" : "green"));
        sb.append(buildCard("Code Smells", String.valueOf(codeSmellCount), "orange"));
        sb.append(buildCard("Security", String.valueOf(securityCount), securityCount > 0 ? "red" : "green"));
        sb.append("</div>\n");

        // Coverage Section
        sb.append("<div class=\"section\">\n<h2>Comment Coverage</h2>\n");
        sb.append("<div class=\"coverage-grid\">\n");
        sb.append(buildCoverageBar("Classes", classCoverage));
        sb.append(buildCoverageBar("Methods", methodCoverage));
        sb.append(buildCoverageBar("Fields", fieldCoverage));
        sb.append("</div>\n</div>\n");

        // Issues by Category Chart
        sb.append("<div class=\"section\">\n<h2>Issues by Category</h2>\n");
        sb.append("<div class=\"chart\">\n");
        sb.append(buildBarChart("BUG", bugCount, "red"));
        sb.append(buildBarChart("CODE_SMELL", codeSmellCount, "orange"));
        sb.append(buildBarChart("SECURITY", securityCount, "purple"));
        sb.append("</div>\n</div>\n");

        // Top Issues Table
        sb.append("<div class=\"section\">\n<h2>Top Issues (").append(Math.min(50, totalIssues)).append(")</h2>\n");
        sb.append("<div class=\"table-wrapper\">\n");
        sb.append("<table>\n<thead><tr><th>Severity</th><th>Category</th><th>Rule</th><th>Class</th><th>Method</th><th>Line</th><th>Message</th></tr></thead>\n<tbody>\n");

        List<Map<String, Object>> sortedIssues = qualityIssues.stream()
                .sorted((a, b) -> {
                    String sevA = (String) a.getOrDefault("severity", "INFO");
                    String sevB = (String) b.getOrDefault("severity", "INFO");
                    return severityOrder(sevA) - severityOrder(sevB);
                })
                .limit(50)
                .collect(Collectors.toList());

        for (Map<String, Object> issue : sortedIssues) {
            String severity = (String) issue.getOrDefault("severity", "INFO");
            String category = (String) issue.getOrDefault("category", "");
            String ruleKey = (String) issue.getOrDefault("rule_key", "");
            String ruleName = (String) issue.getOrDefault("rule_name", "");
            String cls = (String) issue.getOrDefault("class", "");
            String method = (String) issue.getOrDefault("method", "");
            int line = ((Number) issue.getOrDefault("line", 0)).intValue();
            String message = (String) issue.getOrDefault("message", "");

            sb.append("<tr class=\"").append(severity.toLowerCase()).append("\">");
            sb.append("<td><span class=\"badge ").append(severity.toLowerCase()).append("\">").append(severity).append("</span></td>");
            sb.append("<td>").append(category).append("</td>");
            sb.append("<td>").append(ruleKey).append("</td>");
            sb.append("<td>").append(shortName(cls)).append("</td>");
            sb.append("<td>").append(method).append("</td>");
            sb.append("<td>").append(line > 0 ? line : "-").append("</td>");
            sb.append("<td>").append(escapeHtml(message.length() > 100 ? message.substring(0, 100) + "..." : message)).append("</td>");
            sb.append("</tr>\n");
        }

        sb.append("</tbody>\n</table>\n</div>\n</div>\n");

        sb.append("</div>\n"); // container

        // Footer
        sb.append("<div class=\"footer\">\n");
        sb.append("<p>Generated by Java Source Analyzer | ").append(scanDate).append("</p>\n");
        sb.append("</div>\n");

        sb.append("</body>\n</html>");
        return sb.toString();
    }

    private int severityOrder(String severity) {
        switch (severity) {
            case "CRITICAL": return 0;
            case "MAJOR": return 1;
            case "MINOR": return 2;
            default: return 3;
        }
    }

    private String buildCard(String title, String value, String color) {
        return "<div class=\"card\" style=\"border-left: 4px solid var(--" + color + ")\">" +
               "<div class=\"card-title\">" + title + "</div>" +
               "<div class=\"card-value\" style=\"color: var(--" + color + ")\">" + value + "</div></div>\n";
    }

    private String buildCoverageBar(String label, double pct) {
        String color = pct >= 80 ? "var(--green)" : pct >= 50 ? "var(--orange)" : "var(--red)";
        return "<div class=\"coverage-item\"><div class=\"coverage-label\">" + label + "</div>" +
               "<div class=\"coverage-bar-bg\"><div class=\"coverage-bar\" style=\"width:" + pct + "%;background:" + color + "\"></div></div>" +
               "<div class=\"coverage-pct\">" + String.format("%.1f%%", pct) + "</div></div>\n";
    }

    private String buildBarChart(String label, int count, String color) {
        int maxCount = Math.max(count, 1);
        return "<div class=\"bar-item\"><span class=\"bar-label\">" + label + "</span>" +
               "<div class=\"bar-bg\"><div class=\"bar\" style=\"width:" + (count > 0 ? Math.max(count * 100 / Math.max(maxCount, 1), 5) : 0) +
               "%;background:var(--" + color + ")\"></div></div>" +
               "<span class=\"bar-value\">" + count + "</span></div>\n";
    }

    private String shortName(String fullName) {
        if (fullName == null || fullName.isEmpty()) return "-";
        int dotIdx = fullName.lastIndexOf('.');
        return dotIdx > 0 ? fullName.substring(dotIdx + 1) : fullName;
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                   .replace("\"", "&quot;").replace("'", "&#x27;");
    }

    private String getStyles() {
        return "" +
            ":root{--red:#e74c3c;--orange:#f39c12;--blue:#3498db;--green:#27ae60;--purple:#9b59b6;--bg:#f5f6fa;--card:#fff;--text:#2c3e50;--border:#dcdde1}\n" +
            "*{margin:0;padding:0;box-sizing:border-box}\n" +
            "body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;background:var(--bg);color:var(--text);line-height:1.6}\n" +
            ".header{background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);color:#fff;padding:2rem;text-align:center}\n" +
            ".header h1{font-size:2.5rem;margin-bottom:.5rem}\n" +
            ".subtitle{opacity:.9;font-size:1rem}\n" +
            ".container{max-width:1200px;margin:2rem auto;padding:0 1rem}\n" +
            ".cards{display:grid;grid-template-columns:repeat(auto-fit,minmax(150px,1fr));gap:1rem;margin-bottom:2rem}\n" +
            ".card{background:var(--card);border-radius:8px;padding:1.5rem;box-shadow:0 2px 4px rgba(0,0,0,.1)}\n" +
            ".card-title{font-size:.85rem;color:#7f8c8d;text-transform:uppercase;letter-spacing:.5px}\n" +
            ".card-value{font-size:2.5rem;font-weight:700;margin-top:.25rem}\n" +
            ".section{background:var(--card);border-radius:8px;padding:1.5rem;margin-bottom:2rem;box-shadow:0 2px 4px rgba(0,0,0,.1)}\n" +
            ".section h2{font-size:1.25rem;margin-bottom:1rem;padding-bottom:.5rem;border-bottom:2px solid var(--border)}\n" +
            ".coverage-grid{display:flex;flex-direction:column;gap:1rem}\n" +
            ".coverage-item{display:flex;align-items:center;gap:1rem}\n" +
            ".coverage-label{width:80px;font-weight:600;text-align:right}\n" +
            ".coverage-bar-bg{flex:1;background:#ecf0f1;border-radius:4px;height:24px;overflow:hidden}\n" +
            ".coverage-bar{height:100%;border-radius:4px;transition:width .5s}\n" +
            ".coverage-pct{width:60px;font-weight:700}\n" +
            ".chart{display:flex;flex-direction:column;gap:.75rem}\n" +
            ".bar-item{display:flex;align-items:center;gap:1rem}\n" +
            ".bar-label{width:100px;font-weight:600;text-align:right}\n" +
            ".bar-bg{flex:1;background:#ecf0f1;border-radius:4px;height:20px;overflow:hidden}\n" +
            ".bar{height:100%;border-radius:4px;transition:width .5s}\n" +
            ".bar-value{width:40px;font-weight:700}\n" +
            ".table-wrapper{overflow-x:auto}\n" +
            "table{width:100%;border-collapse:collapse}\n" +
            "th{background:#f8f9fa;padding:.75rem;text-align:left;font-size:.85rem;text-transform:uppercase;letter-spacing:.5px;color:#7f8c8d}\n" +
            "td{padding:.75rem;border-top:1px solid var(--border);font-size:.9rem}\n" +
            "tr.critical{background:#ffeaa7}\n" +
            "tr.major{background:#fff3cd}\n" +
            "tr.minor{background:#f8f9fa}\n" +
            ".badge{display:inline-block;padding:.25rem .5rem;border-radius:4px;font-size:.75rem;font-weight:700;color:#fff}\n" +
            ".badge.critical{background:var(--red)}\n" +
            ".badge.major{background:var(--orange)}\n" +
            ".badge.minor{background:var(--blue)}\n" +
            ".footer{text-align:center;padding:2rem;color:#7f8c8d;font-size:.85rem}\n";
    }
}
