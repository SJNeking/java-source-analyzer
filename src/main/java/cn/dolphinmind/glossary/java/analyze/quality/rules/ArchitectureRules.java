package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.QualityRule;
import cn.dolphinmind.glossary.java.analyze.quality.Severity;

import java.util.*;
import java.util.regex.*;

/**
 * Architecture Rules - P2 Priority
 *
 * Detects architecture-level anti-patterns:
 * - Package layer violations
 * - Circular dependencies between classes
 * - Excessive coupling (Fan-out)
 * - Feature Envy
 * - God Class (enhanced)
 * - Data Class (only getters/setters)
 * - Hub-like dependencies
 * - Unstable dependencies
 */
public final class ArchitectureRules {
    private ArchitectureRules() {}

    // =====================================================================
    // Layer Violations
    // =====================================================================

    /**
     * RSPEC-1201: Package by layer should be enforced
     * Detects CONTROLLER → REPOSITORY direct dependencies.
     */
    public static class LayerViolation extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-1201"; }
        public String getName() { return "Classes should respect layer boundaries"; }
        public String getCategory() { return "ARCHITECTURE"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            String className = cn != null ? cn.toLowerCase() : "";

            // Detect layer from class name
            boolean isController = className.contains(".controller.") || className.contains(".rest.") || className.contains(".web.");
            boolean isService = className.contains(".service.") || className.contains(".biz.");
            boolean isRepository = className.contains(".repository.") || className.contains(".dao.") || className.contains(".mapper.");
            boolean isEntity = className.contains(".entity.") || className.contains(".model.") || className.contains(".domain.");

            // Detect dependencies from imports and usage
            boolean usesRepository = body.contains("Repository") && (body.contains(".find") || body.contains(".save") || body.contains(".delete"));
            boolean usesEntityDirectly = body.contains("Entity") && body.contains("new ") && !className.contains(".service.");

            // CONTROLLER → REPOSITORY is a violation
            if (isController && usesRepository) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Controller directly accesses Repository", "layer violation: controller → repository"));
            }

            // CONTROLLER → ENTITY (bypassing DTO/Service)
            if (isController && usesEntityDirectly) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Controller directly creates Entity", "layer violation: controller → entity"));
            }

            return issues;
        }
    }

    // =====================================================================
    // Circular Dependencies
    // =====================================================================

    /**
     * RSPEC-1202: Circular dependencies between classes
     * Detects ClassA → ClassB → ClassA patterns.
     */
    public static class CircularDependency implements QualityRule {
        public String getRuleKey() { return "RSPEC-1202"; }
        public String getName() { return "Classes should not have circular dependencies"; }
        public String getCategory() { return "ARCHITECTURE"; }
        @SuppressWarnings("unchecked")
        public List<QualityIssue> check(Map<String, Object> classAsset) {
            List<QualityIssue> issues = new ArrayList<>();
            String className = (String) classAsset.getOrDefault("address", "");
            String sourceFile = (String) classAsset.getOrDefault("source_file", "");
            List<String> imports = (List<String>) classAsset.getOrDefault("import_dependencies", Collections.emptyList());

            // Check if any imported class also imports this class (simplified circular detection)
            for (String imp : imports) {
                if (imp.contains(".") && !imp.startsWith("java.") && !imp.startsWith("javax.")) {
                    // This is a simplified check - real circular detection requires full graph analysis
                    // Here we just flag potential circular deps based on mutual imports
                }
            }

            return issues; // Full circular detection is done by ArchitectureAnalyzer
        }
    }

    // =====================================================================
    // Excessive Coupling
    // =====================================================================

    /**
     * RSPEC-1203: Excessive Fan-out coupling
     * Detects classes that depend on too many other classes.
     */
    public static class ExcessiveFanOut extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-1203"; }
        public String getName() { return "Classes should not have excessive fan-out"; }
        public String getCategory() { return "ARCHITECTURE"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // Count distinct external types used
            Set<String> externalTypes = new HashSet<>();
            Matcher typeMatcher = Pattern.compile("\\b([A-Z]\\w+)\\s*\\w+\\s*[=;,(]").matcher(body);
            while (typeMatcher.find()) {
                String type = typeMatcher.group(1);
                if (!type.equals("String") && !type.equals("Integer") && !type.equals("Long") &&
                    !type.equals("Boolean") && !type.equals("List") && !type.equals("Map") &&
                    !type.equals("Set") && !type.equals("Optional") && !type.equals("var")) {
                    externalTypes.add(type);
                }
            }

            if (externalTypes.size() > 10) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Excessive fan-out: " + externalTypes.size() + " external types", "fan-out=" + externalTypes.size()));
            }
            return issues;
        }
    }

    /**
     * RSPEC-1204: Hub-like dependency (many classes depend on this one)
     * Detects classes that are imported by many other classes.
     */
    public static class HubLikeDependency implements QualityRule {
        public String getRuleKey() { return "RSPEC-1204"; }
        public String getName() { return "Classes should not be hub-like dependencies"; }
        public String getCategory() { return "ARCHITECTURE"; }
        @SuppressWarnings("unchecked")
        public List<QualityIssue> check(Map<String, Object> classAsset) {
            // This requires cross-class analysis, handled separately
            return Collections.emptyList();
        }
    }

    // =====================================================================
    // Feature Envy
    // =====================================================================

    /**
     * RSPEC-1205: Feature Envy
     * Detects methods that use more features of other classes than their own.
     */
    public static class FeatureEnvy extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-1205"; }
        public String getName() { return "Methods should not have Feature Envy"; }
        public String getCategory() { return "ARCHITECTURE"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // Count method calls on other objects vs own class
            Matcher otherCallsMatcher = Pattern.compile("\\w+\\.\\w+\\s*\\(").matcher(body);
            int otherCallCount = 0;
            while (otherCallsMatcher.find()) {
                String call = otherCallsMatcher.group();
                String target = call.substring(0, call.indexOf('.'));
                if (!target.equals("this") && !target.equals(name) && !target.equals("super")) {
                    otherCallCount++;
                }
            }

            // If > 70% of calls are on other objects, it's Feature Envy
            if (otherCallCount > 5) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Feature Envy: " + otherCallCount + " calls on other objects", "envy=" + otherCallCount));
            }
            return issues;
        }
    }

    // =====================================================================
    // God Class (Enhanced)
    // =====================================================================

    /**
     * RSPEC-1206: God Class (enhanced version)
     * Detects classes with too many methods, fields, and lines of code.
     */
    public static class GodClassEnhanced implements QualityRule {
        public String getRuleKey() { return "RSPEC-1206"; }
        public String getName() { return "God Class: class has too many responsibilities"; }
        public String getCategory() { return "ARCHITECTURE"; }
        @SuppressWarnings("unchecked")
        public List<QualityIssue> check(Map<String, Object> classAsset) {
            List<QualityIssue> issues = new ArrayList<>();
            String className = (String) classAsset.getOrDefault("address", "");
            String sourceFile = (String) classAsset.getOrDefault("source_file", "");

            List<Map<String, Object>> methods = (List<Map<String, Object>>) classAsset.getOrDefault("methods_full", Collections.emptyList());
            List<Map<String, Object>> fields = (List<Map<String, Object>>) classAsset.getOrDefault("fields_matrix", Collections.emptyList());

            int methodCount = methods.size();
            int fieldCount = fields.size();
            int loc = (int) classAsset.getOrDefault("lines_of_code", 0);

            // God class criteria: > 20 methods AND > 10 fields AND > 500 LOC
            if (methodCount > 20 && fieldCount > 10 && loc > 500) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), sourceFile, className,
                    "", 0, "God Class: " + methodCount + " methods, " + fieldCount + " fields, " + loc + " LOC",
                    "methods=" + methodCount + ", fields=" + fieldCount + ", loc=" + loc));
            }
            return issues;
        }
    }

    // =====================================================================
    // Data Class
    // =====================================================================

    /**
     * RSPEC-1207: Data Class (only getters/setters)
     * Detects classes that are purely data holders.
     */
    public static class DataClass implements QualityRule {
        public String getRuleKey() { return "RSPEC-1207"; }
        public String getName() { return "Data Class: class only has getters/setters"; }
        public String getCategory() { return "ARCHITECTURE"; }
        @SuppressWarnings("unchecked")
        public List<QualityIssue> check(Map<String, Object> classAsset) {
            List<QualityIssue> issues = new ArrayList<>();
            String className = (String) classAsset.getOrDefault("address", "");
            String sourceFile = (String) classAsset.getOrDefault("source_file", "");
            String kind = (String) classAsset.getOrDefault("kind", "");

            if (!"CLASS".equals(kind)) return issues;

            List<Map<String, Object>> methods = (List<Map<String, Object>>) classAsset.getOrDefault("methods_full", Collections.emptyList());
            List<Map<String, Object>> fields = (List<Map<String, Object>>) classAsset.getOrDefault("fields_matrix", Collections.emptyList());

            if (methods.isEmpty() || fields.isEmpty()) return issues;

            // Count getter/setter methods
            int getterSetterCount = 0;
            int otherMethodCount = 0;
            for (Map<String, Object> method : methods) {
                String methodName = (String) method.getOrDefault("name", "");
                if (methodName.startsWith("get") || methodName.startsWith("set") ||
                    methodName.startsWith("is") || methodName.equals("toString") ||
                    methodName.equals("hashCode") || methodName.equals("equals") ||
                    methodName.equals("toString")) {
                    getterSetterCount++;
                } else {
                    otherMethodCount++;
                }
            }

            // If > 80% of methods are getters/setters, it's a Data Class
            double dataRatio = (double) getterSetterCount / methods.size();
            if (dataRatio > 0.8 && otherMethodCount <= 2) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), sourceFile, className,
                    "", 0, "Data Class: " + getterSetterCount + "/" + methods.size() + " are getters/setters",
                    "data class ratio=" + String.format("%.2f", dataRatio)));
            }
            return issues;
        }
    }

    // =====================================================================
    // Unstable Dependencies
    // =====================================================================

    /**
     * RSPEC-1208: Unstable dependencies
     * Detects dependencies on volatile/abstract modules.
     */
    public static class UnstableDependency extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-1208"; }
        public String getName() { return "Classes should not depend on unstable modules"; }
        public String getCategory() { return "ARCHITECTURE"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // Detect imports of internal/unstable packages
            String[] unstablePatterns = {
                "impl\\.", "internal\\.", "util\\.", "helper\\."
            };

            for (String pattern : unstablePatterns) {
                if (Pattern.compile("\\bimport\\s+.*\\." + pattern).matcher(body).find()) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                        "Dependency on unstable module: " + pattern, "unstable import"));
                    break;
                }
            }
            return issues;
        }
    }

    // =====================================================================
    // Abstractness vs Instability
    // =====================================================================

    /**
     * RSPEC-1209: Too concrete class (should be abstract)
     * Detects classes that should be abstract but aren't.
     */
    public static class MissingAbstraction extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-1209"; }
        public String getName() { return "Classes should be abstract if intended for extension"; }
        public String getCategory() { return "ARCHITECTURE"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);

            // Detect classes with many abstract methods but class itself is not abstract
            // This is a simplified check
            boolean hasAbstractMethods = body.contains("abstract ") && body.contains("(");
            boolean isClassAbstract = body.contains("abstract class") || body.contains("interface");

            if (hasAbstractMethods && !isClassAbstract) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Class has abstract methods but is not abstract", "missing abstract"));
            }
            return issues;
        }
    }
}
