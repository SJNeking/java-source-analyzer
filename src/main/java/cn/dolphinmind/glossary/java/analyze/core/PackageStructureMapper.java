package cn.dolphinmind.glossary.java.analyze.core;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core Feature 3: Package Structure Mapper
 *
 * Automatically generates a project's package structure with layer inference.
 * Answers:
 * - What are the main packages?
 * - What layer does each package belong to? (controller, service, repository, etc.)
 * - How many classes per package?
 * - What are the dependencies between packages?
 */
public class PackageStructureMapper {

    /**
     * A single package node in the structure.
     */
    public static class PackageNode {
        private final String packageName;
        private final String layer;
        private final List<String> classes = new ArrayList<>();
        private final Map<String, PackageNode> subPackages = new LinkedHashMap<>();
        private final Set<String> dependencies = new LinkedHashSet<>();

        public PackageNode(String packageName) {
            this.packageName = packageName;
            this.layer = inferLayer(packageName);
        }

        public String getPackageName() { return packageName; }
        public String getLayer() { return layer; }
        public List<String> getClasses() { return Collections.unmodifiableList(classes); }
        public Map<String, PackageNode> getSubPackages() { return Collections.unmodifiableMap(subPackages); }
        public Set<String> getDependencies() { return Collections.unmodifiableSet(dependencies); }

        public int getTotalClassCount() {
            int count = classes.size();
            for (PackageNode sub : subPackages.values()) {
                count += sub.getTotalClassCount();
            }
            return count;
        }

        public void addClass(String className) {
            classes.add(className);
        }

        public PackageNode getOrCreateSubPackage(String subName) {
            return subPackages.computeIfAbsent(subName, PackageNode::new);
        }

        public void addDependency(String targetPackage) {
            if (!targetPackage.equals(packageName)) {
                dependencies.add(targetPackage);
            }
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("package", packageName);
            map.put("layer", layer);
            map.put("class_count", classes.size());
            map.put("total_class_count", getTotalClassCount());
            if (!subPackages.isEmpty()) {
                map.put("sub_packages", subPackages.values().stream()
                        .map(PackageNode::toMap).collect(Collectors.toList()));
            }
            if (!classes.isEmpty()) {
                map.put("classes", classes);
            }
            if (!dependencies.isEmpty()) {
                map.put("dependencies", dependencies);
            }
            return map;
        }
    }

    /**
     * Build package structure from a project.
     */
    public PackageNode build(Path projectRoot) throws IOException {
        Map<String, PackageNode> packageMap = new LinkedHashMap<>();

        Files.walk(projectRoot)
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> p.toString().contains("src"))
                .filter(p -> !p.toString().contains("test"))
                .filter(p -> !p.toString().contains("target"))
                .forEach(path -> {
                    try {
                        String content = new String(Files.readAllBytes(path));
                        String packageName = extractPackage(content);
                        String className = extractClassName(content);
                        String fullClassName = (packageName.isEmpty() ? "" : packageName + ".") + className;

                        // Add to package tree
                        PackageNode root = getOrCreateRoot(packageMap, packageName);
                        addClassToTree(root, packageName, className);

                        // Extract dependencies (import statements)
                        List<String> imports = extractImports(content);
                        for (String imp : imports) {
                            String impPackage = extractPackageFromImport(imp);
                            root.addDependency(impPackage);
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                });

        // Return the root node
        return packageMap.values().isEmpty() ? new PackageNode("default") : packageMap.values().iterator().next();
    }

    /**
     * Infer the architectural layer from package name.
     */
    public static String inferLayer(String packageName) {
        String lower = packageName.toLowerCase();
        if (lower.contains(".controller") || lower.contains(".web") || lower.contains(".rest") || lower.contains(".api"))
            return "CONTROLLER";
        if (lower.contains(".service") || lower.contains(".biz") || lower.contains(".domain"))
            return "SERVICE";
        if (lower.contains(".repository") || lower.contains(".mapper") || lower.contains(".dao") ||
            lower.contains(".jpa") || lower.contains(".jdbc"))
            return "REPOSITORY";
        if (lower.contains(".entity") || lower.contains(".model") || lower.contains(".domain.entity") ||
            lower.contains(".po") || lower.contains(".pojo") || lower.contains(".bean"))
            return "ENTITY";
        if (lower.contains(".dto") || lower.contains(".vo") || lower.contains(".request") ||
            lower.contains(".response") || lower.contains(".command") || lower.contains(".query"))
            return "DTO";
        if (lower.contains(".config") || lower.contains(".configuration"))
            return "CONFIG";
        if (lower.contains(".util") || lower.contains(".common") || lower.contains(".helper"))
            return "UTIL";
        if (lower.contains(".listener") || lower.contains(".consumer") || lower.contains(".handler"))
            return "HANDLER";
        if (lower.contains(".job") || lower.contains(".task") || lower.contains(".schedule"))
            return "SCHEDULED";
        if (lower.contains(".filter") || lower.contains(".interceptor") || lower.contains(".aspect"))
            return "MIDDLEWARE";
        if (lower.contains(".exception") || lower.contains(".error"))
            return "EXCEPTION";
        if (lower.contains(".app") || lower.contains(".starter") || lower.endsWith(".main"))
            return "APPLICATION";
        return "OTHER";
    }

    /**
     * Print the package structure as a tree.
     */
    public void printTree(PackageNode root) {
        printNode(root, "", true);
    }

    private void printNode(PackageNode node, String prefix, boolean isTail) {
        String icon = isTail ? "└── " : "├── ";
        String layerInfo = node.getLayer().isEmpty() ? "" : " [" + node.getLayer() + "]";
        System.out.println(prefix + icon + node.getPackageName() + layerInfo +
                " (" + node.getTotalClassCount() + " classes)");

        String childPrefix = prefix + (isTail ? "    " : "│   ");
        List<PackageNode> subs = new ArrayList<>(node.getSubPackages().values());
        for (int i = 0; i < subs.size(); i++) {
            printNode(subs.get(i), childPrefix, i == subs.size() - 1);
        }
    }

    /**
     * Export as JSON-compatible map.
     */
    public Map<String, Object> export(PackageNode root) {
        return root.toMap();
    }

    // ---- Private helpers ----

    private PackageNode getOrCreateRoot(Map<String, PackageNode> packageMap, String packageName) {
        String rootPkg = packageName.isEmpty() ? "default" : packageName.split("\\.")[0];
        return packageMap.computeIfAbsent(rootPkg, PackageNode::new);
    }

    private void addClassToTree(PackageNode root, String packageName, String className) {
        if (packageName.isEmpty()) {
            root.addClass(className);
            return;
        }

        String[] parts = packageName.split("\\.");
        PackageNode current = root;
        for (int i = 1; i < parts.length; i++) {
            current = current.getOrCreateSubPackage(parts[i]);
        }
        current.addClass(className);
    }

    private String extractPackage(String content) {
        int idx = content.indexOf("package ");
        if (idx < 0) return "";
        int semiIdx = content.indexOf(';', idx);
        if (semiIdx < 0) return "";
        return content.substring(idx + 8, semiIdx).trim();
    }

    private String extractClassName(String content) {
        for (String prefix : Arrays.asList("public class ", "public interface ", "public abstract class ",
                                            "class ", "interface ", "abstract class ", "enum ")) {
            int idx = content.indexOf(prefix);
            if (idx >= 0) {
                int start = idx + prefix.length();
                int end = content.indexOf(' ', start);
                if (end < 0) end = content.indexOf('{', start);
                if (end > start) return content.substring(start, end).trim();
            }
        }
        return "Unknown";
    }

    private List<String> extractImports(String content) {
        List<String> imports = new ArrayList<>();
        String[] lines = content.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("import ") && !trimmed.contains("static")) {
                int semiIdx = trimmed.indexOf(';');
                if (semiIdx > 0) {
                    imports.add(trimmed.substring(7, semiIdx).trim());
                }
            }
        }
        return imports;
    }

    private String extractPackageFromImport(String imp) {
        int lastDot = imp.lastIndexOf('.');
        return lastDot > 0 ? imp.substring(0, lastDot) : imp;
    }
}
