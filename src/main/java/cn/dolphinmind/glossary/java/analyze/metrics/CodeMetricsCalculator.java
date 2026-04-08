package cn.dolphinmind.glossary.java.analyze.metrics;

import cn.dolphinmind.glossary.java.analyze.core.CallChainTracer;
import cn.dolphinmind.glossary.java.analyze.core.PackageStructureMapper;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Comprehensive Code Metrics Calculator
 *
 *对标 JArchitect 的代码指标体系：
 * - 基本指标：类数、方法数、字段数、行数
 * - 复杂度指标：圈复杂度、继承深度、嵌套深度
 * - 耦合指标：Fan-in/Fan-out、 afferent/efferent coupling
 * - 内聚指标：方法内聚度
 * - 规模指标：注释率、平均方法长度
 */
public class CodeMetricsCalculator {

    /**
     * Complete metrics for a project.
     */
    public static class ProjectMetrics {
        public int totalClasses;
        public int totalMethods;
        public int totalFields;
        public int totalLinesOfCode;
        public int totalCommentLines;
        public double averageMethodLength;
        public double commentRatio;
        public double averageComplexity;
        public double maxComplexity;
        public double averageInheritanceDepth;
        public double maxInheritanceDepth;
        public double averageCoupling;
        public double maxCoupling;
        public double cohesionIndex;
        public int abstractClasses;
        public int interfaces;
        public int enumClasses;

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("total_classes", totalClasses);
            map.put("total_methods", totalMethods);
            map.put("total_fields", totalFields);
            map.put("total_loc", totalLinesOfCode);
            map.put("comment_lines", totalCommentLines);
            map.put("comment_ratio", commentRatio);
            map.put("avg_method_length", averageMethodLength);
            map.put("avg_complexity", averageComplexity);
            map.put("max_complexity", maxComplexity);
            map.put("avg_inheritance_depth", averageInheritanceDepth);
            map.put("max_inheritance_depth", maxInheritanceDepth);
            map.put("avg_coupling", averageCoupling);
            map.put("max_coupling", maxCoupling);
            map.put("cohesion_index", cohesionIndex);
            map.put("abstract_classes", abstractClasses);
            map.put("interfaces", interfaces);
            map.put("enum_classes", enumClasses);
            return map;
        }
    }

    /**
     * Per-class metrics.
     */
    public static class ClassMetrics {
        public String className;
        public String layer;
        public int methodCount;
        public int fieldCount;
        public int loc;
        public double complexity;
        public int inheritanceDepth;
        public int fanIn;  // 多少其他类依赖这个类
        public int fanOut; // 这个类依赖多少其他类
        public double cohesion;
        public int commentLines;

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("class", className);
            map.put("layer", layer);
            map.put("methods", methodCount);
            map.put("fields", fieldCount);
            map.put("loc", loc);
            map.put("complexity", complexity);
            map.put("inheritance_depth", inheritanceDepth);
            map.put("fan_in", fanIn);
            map.put("fan_out", fanOut);
            map.put("cohesion", cohesion);
            map.put("comments", commentLines);
            return map;
        }
    }

    /**
     * Calculate project-level metrics.
     */
    public ProjectMetrics calculateProjectMetrics(
            List<Map<String, Object>> classAssets,
            CallChainTracer.CallGraph callGraph,
            PackageStructureMapper.PackageNode packageTree) {

        ProjectMetrics metrics = new ProjectMetrics();
        List<ClassMetrics> classMetricsList = new ArrayList<>();

        // Calculate per-class metrics first
        for (Map<String, Object> asset : classAssets) {
            ClassMetrics cm = calculateClassMetrics(asset, callGraph);
            classMetricsList.add(cm);

            metrics.totalClasses++;
            metrics.totalMethods += cm.methodCount;
            metrics.totalFields += cm.fieldCount;
            metrics.totalLinesOfCode += cm.loc;
            metrics.totalCommentLines += cm.commentLines;

            if (asset.containsKey("is_abstract") && Boolean.TRUE.equals(asset.get("is_abstract"))) {
                metrics.abstractClasses++;
            }
            if (asset.containsKey("is_interface") && Boolean.TRUE.equals(asset.get("is_interface"))) {
                metrics.interfaces++;
            }
            if (asset.containsKey("is_enum") && Boolean.TRUE.equals(asset.get("is_enum"))) {
                metrics.enumClasses++;
            }
        }

        // Calculate averages
        if (metrics.totalClasses > 0) {
            metrics.averageMethodLength = (double) metrics.totalMethods / metrics.totalClasses;
            metrics.commentRatio = (double) metrics.totalCommentLines / Math.max(metrics.totalLinesOfCode, 1);
            metrics.averageComplexity = classMetricsList.stream().mapToDouble(c -> c.complexity).average().orElse(0);
            metrics.maxComplexity = classMetricsList.stream().mapToDouble(c -> c.complexity).max().orElse(0);
            metrics.averageInheritanceDepth = classMetricsList.stream().mapToInt(c -> c.inheritanceDepth).average().orElse(0);
            metrics.maxInheritanceDepth = classMetricsList.stream().mapToInt(c -> c.inheritanceDepth).max().orElse(0);
            metrics.averageCoupling = classMetricsList.stream().mapToDouble(c -> c.fanIn + c.fanOut).average().orElse(0);
            metrics.maxCoupling = classMetricsList.stream().mapToDouble(c -> c.fanIn + c.fanOut).max().orElse(0);
        }

        // Calculate cohesion index (0-1, higher = better)
        metrics.cohesionIndex = calculateCohesionIndex(classMetricsList);

        return metrics;
    }

    /**
     * Calculate per-class metrics.
     */
    public ClassMetrics calculateClassMetrics(Map<String, Object> classAsset, CallChainTracer.CallGraph callGraph) {
        ClassMetrics cm = new ClassMetrics();

        cm.className = (String) classAsset.getOrDefault("address", "");
        cm.layer = (String) classAsset.getOrDefault("layer", "");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> methods = (List<Map<String, Object>>) classAsset.getOrDefault("methods_full", Collections.emptyList());
        cm.methodCount = methods.size();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fields = (List<Map<String, Object>>) classAsset.getOrDefault("fields_matrix", Collections.emptyList());
        cm.fieldCount = fields.size();

        cm.loc = (int) classAsset.getOrDefault("lines_of_code", 0);
        cm.complexity = (double) classAsset.getOrDefault("cyclomatic_complexity", 1.0);
        cm.inheritanceDepth = (int) classAsset.getOrDefault("inheritance_depth", 0);
        cm.commentLines = (int) classAsset.getOrDefault("comment_lines", 0);

        // Calculate fan-in and fan-out from call graph
        String className = cm.className;
        int fanIn = 0, fanOut = 0;

        if (callGraph != null) {
            // Fan-out: methods in this class calling other classes
            for (Map.Entry<String, Set<String>> entry : callGraph.getAdjacencyList().entrySet()) {
                if (entry.getKey().startsWith(className + "#")) {
                    fanOut += entry.getValue().stream()
                            .filter(callee -> !callee.startsWith(className + "#"))
                            .count();
                }
            }
            // Fan-in: other classes calling methods in this class
            for (Map.Entry<String, Set<String>> entry : callGraph.getAdjacencyList().entrySet()) {
                if (!entry.getKey().startsWith(className + "#")) {
                    fanIn += entry.getValue().stream()
                            .filter(callee -> callee.startsWith(className + "#"))
                            .count();
                }
            }
        }

        cm.fanIn = fanIn;
        cm.fanOut = fanOut;

        // Calculate cohesion: LCOM (Lack of Cohesion of Methods) simplified
        cm.cohesion = calculateClassCohesion(methods, fields);

        return cm;
    }

    /**
     * Calculate class cohesion (0-1, 1 = perfect cohesion).
     * Simplified: ratio of fields used by methods to total fields.
     */
    private double calculateClassCohesion(List<Map<String, Object>> methods, List<Map<String, Object>> fields) {
        if (methods.isEmpty() || fields.isEmpty()) return 1.0;

        Set<String> usedFields = new HashSet<>();
        for (Map<String, Object> method : methods) {
            String body = (String) method.getOrDefault("body_code", "");
            for (Map<String, Object> field : fields) {
                String fieldName = (String) field.getOrDefault("name", "");
                if (!fieldName.isEmpty() && body.contains(fieldName)) {
                    usedFields.add(fieldName);
                }
            }
        }

        return (double) usedFields.size() / fields.size();
    }

    /**
     * Calculate overall project cohesion index.
     */
    private double calculateCohesionIndex(List<ClassMetrics> classMetrics) {
        if (classMetrics.isEmpty()) return 0.0;
        return classMetrics.stream()
                .mapToDouble(c -> c.cohesion)
                .average()
                .orElse(0.0);
    }

    /**
     * Generate a summary report.
     */
    public void printMetricsSummary(ProjectMetrics metrics) {
        System.out.println("\n=== 代码指标报告 ===");
        System.out.println("📊 项目规模:");
        System.out.println("  类总数: " + metrics.totalClasses);
        System.out.println("  方法总数: " + metrics.totalMethods);
        System.out.println("  字段总数: " + metrics.totalFields);
        System.out.println("  代码行数: " + metrics.totalLinesOfCode);
        System.out.println("  注释行数: " + metrics.totalCommentLines + " (" + String.format("%.1f", metrics.commentRatio * 100) + "%)");
        System.out.println("  抽象类: " + metrics.abstractClasses);
        System.out.println("  接口: " + metrics.interfaces);
        System.out.println("  枚举: " + metrics.enumClasses);

        System.out.println("\n📈 复杂度:");
        System.out.println("  平均圈复杂度: " + String.format("%.2f", metrics.averageComplexity));
        System.out.println("  最大圈复杂度: " + String.format("%.2f", metrics.maxComplexity));
        System.out.println("  平均方法长度: " + String.format("%.2f", metrics.averageMethodLength));
        System.out.println("  平均继承深度: " + String.format("%.2f", metrics.averageInheritanceDepth));

        System.out.println("\n🔗 耦合度:");
        System.out.println("  平均耦合: " + String.format("%.2f", metrics.averageCoupling));
        System.out.println("  最大耦合: " + String.format("%.2f", metrics.maxCoupling));

        System.out.println("\n🎯 内聚度:");
        System.out.println("  项目内聚指数: " + String.format("%.2f", metrics.cohesionIndex));
    }
}
