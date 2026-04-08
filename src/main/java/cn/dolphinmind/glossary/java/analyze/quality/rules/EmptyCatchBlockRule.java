package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.Severity;

import java.util.*;

/**
 * BUG: Empty catch block - swallows exceptions silently
 */
public class EmptyCatchBlockRule extends AbstractMethodRule {

    @Override
    public String getRuleKey() { return "RSPEC-108"; }

    @Override
    public String getName() { return "Empty catch blocks should not be used"; }

    @Override
    public String getCategory() { return "BUG"; }

    @Override
    protected List<QualityIssue> checkMethod(Map<String, Object> method, String filePath, String className) {
        List<QualityIssue> issues = new ArrayList<>();
        String bodyCode = (String) method.getOrDefault("body_code", "");
        String methodName = (String) method.get("name");
        int line = (int) method.getOrDefault("line_start", 0);

        if (bodyCode.isEmpty()) return issues;

        // Detect empty catch blocks: catch (...) {} or catch (...) { } or catch (...) {\n}
        String[] patterns = {
            "catch\\s*\\([^)]*\\)\\s*\\{\\s*\\}",
            "catch\\s*\\([^)]*\\)\\s*\\{\\s*//[^}]*\\}",
            "catch\\s*\\([^)]*\\)\\s*\\{\\s*/\\*[^}]*\\*/\\s*\\}"
        };

        for (String pattern : patterns) {
            if (java.util.regex.Pattern.compile(pattern).matcher(bodyCode).find()) {
                issues.add(new QualityIssue(
                        getRuleKey(), getName(), Severity.MAJOR, getCategory(),
                        filePath, className, methodName, line,
                        "Empty catch block found - exception is silently swallowed",
                        "catch block in " + methodName
                ));
                break;
            }
        }
        return issues;
    }
}
