package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.Severity;

import java.util.*;
import java.util.regex.*;

/**
 * Code Organization Rules
 *
 * Detects code organization anti-patterns:
 * - Wrong package
 * - Wrong import order
 * - Static import misuse
 * - Wildcard import
 * - Unused import
 * - Duplicate import
 * - Too many imports
 * - Package-info missing
 * - Class order in file
 * - Method order in class
 */
public final class CodeOrganizationRules {
    private CodeOrganizationRules() {}

    /**
     * RSPEC-19001: Wrong package name
     */
    public static class WrongPackageName extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-19001"; }
        public String getName() { return "Package name should follow conventions"; }
        public String getCategory() { return "CODE_ORGANIZATION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("package ") && body.split("package ")[1].contains(".")) {
                String pkg = body.split("package ")[1].split(";")[0];
                if (pkg.contains("impl") || pkg.contains("internal")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "Implementation package", "consider public API"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-19002: Static import misuse
     */
    public static class StaticImportMisuse extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-19002"; }
        public String getName() { return "Static imports should be used carefully"; }
        public String getCategory() { return "CODE_ORGANIZATION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("import static") && body.split("import static").length > 10) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Too many static imports", "confusing namespace"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-19003: Unused import
     */
    public static class UnusedImport extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-19003"; }
        public String getName() { return "Unused import should be removed"; }
        public String getCategory() { return "CODE_ORGANIZATION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            String[] imports = body.split("import ");
            for (int i = 1; i < imports.length && i < 30; i++) {
                String imp = imports[i].split(";")[0];
                if (imp.contains("import static")) continue;
                String[] parts = imp.split("\\.");
                String className = parts[parts.length - 1];
                if (!body.contains(className) && !className.equals("*")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "Unused import: " + imp, "remove unused"));
                    break;
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-19004: Too many imports
     */
    public static class TooManyImports extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-19004"; }
        public String getName() { return "File should not have too many imports"; }
        public String getCategory() { return "CODE_ORGANIZATION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            int importCount = body.split("import ").length - 1;
            if (importCount > 30) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "File has " + importCount + " imports", "refactor to reduce"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-19005: Duplicate import
     */
    public static class DuplicateImport extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-19005"; }
        public String getName() { return "Duplicate import should be removed"; }
        public String getCategory() { return "CODE_ORGANIZATION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            String[] imports = body.split("import ");
            Set<String> seen = new HashSet<>();
            for (int i = 1; i < imports.length; i++) {
                String imp = imports[i].split(";")[0].trim();
                if (seen.contains(imp)) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "Duplicate import: " + imp, "remove duplicate"));
                    break;
                }
                seen.add(imp);
            }
            return issues;
        }
    }

    /**
     * RSPEC-19006: Package-info missing
     */
    public static class PackageInfoMissing extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-19006"; }
        public String getName() { return "Package should have package-info.java"; }
        public String getCategory() { return "CODE_ORGANIZATION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("package ") && !body.contains("package-info")) {
                if (body.split("public class").length > 5 || body.split("public interface").length > 5) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "Package without package-info", "add documentation"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-19007: Wrong class order in file
     */
    public static class WrongClassOrder extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-19007"; }
        public String getName() { return "Public class should be first in file"; }
        public String getCategory() { return "CODE_ORGANIZATION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("class ") && body.split("class ").length > 2) {
                int firstPublic = body.indexOf("public class");
                int firstClass = body.indexOf("class ");
                if (firstPublic > firstClass && firstPublic >= 0) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "Public class not first", "follow convention"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-19008: Method order in class
     */
    public static class WrongMethodOrder extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-19008"; }
        public String getName() { return "Methods should follow logical order"; }
        public String getCategory() { return "CODE_ORGANIZATION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            int publicMethods = body.split("public ").length - 1;
            int privateMethods = body.split("private ").length - 1;
            if (publicMethods > 0 && privateMethods > 0) {
                int firstPrivate = body.indexOf("private ");
                int lastPublic = body.lastIndexOf("public ");
                if (firstPrivate < lastPublic && firstPrivate >= 0) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "Mixed public/private methods", "group by visibility"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-19009: Test class in wrong package
     */
    public static class TestInWrongPackage extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-19009"; }
        public String getName() { return "Test class should match source package"; }
        public String getCategory() { return "CODE_ORGANIZATION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (name.startsWith("test") && fp != null && !fp.contains("test")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Test class in source directory", "move to test directory"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-19010: Nested class too deep
     */
    public static class NestedClassTooDeep extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-19010"; }
        public String getName() { return "Nested classes should not be too deep"; }
        public String getCategory() { return "CODE_ORGANIZATION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.split("class ").length > 3) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Too many nested classes", "extract to separate file"));
            }
            return issues;
        }
    }
}
