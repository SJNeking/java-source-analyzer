package cn.dolphinmind.glossary.java.analyze.core;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core Feature 3: Package Structure Mapper (JavaParser-based)
 *
 * Uses JavaParser AST to build accurate package structure with layer inference.
 */
public class PackageStructureMapper {

    public static class PackageNode {
        private final String packageName;
        private final String fullPackageName;
        private String layer;
        private final List<String> classes = new ArrayList<>();
        private final Map<String, PackageNode> subPackages = new LinkedHashMap<>();
        private final Set<String> dependencies = new LinkedHashSet<>();

        public PackageNode(String packageName, String fullPackageName) {
            this.packageName = packageName;
            this.fullPackageName = fullPackageName;
            this.layer = inferLayer(fullPackageName);
        }

        public PackageNode(String packageName) {
            this(packageName, packageName);
        }

        public String getPackageName() { return packageName; }
        public String getLayer() { return layer; }
        public List<String> getClasses() { return Collections.unmodifiableList(classes); }
        public Map<String, PackageNode> getSubPackages() { return Collections.unmodifiableMap(subPackages); }
        public Set<String> getDependencies() { return Collections.unmodifiableSet(dependencies); }

        public int getTotalClassCount() {
            int count = classes.size();
            for (PackageNode sub : subPackages.values()) count += sub.getTotalClassCount();
            return count;
        }

        public void addClass(String className) { classes.add(className); }
        public void addClasses(List<String> classNames) { classes.addAll(classNames); }

        public PackageNode getOrCreateSubPackage(String subName) {
            String fullFull = fullPackageName.isEmpty() ? subName : fullPackageName + "." + subName;
            return subPackages.computeIfAbsent(subName, k -> new PackageNode(k, fullFull));
        }

        public void addDependency(String targetPackage) {
            if (!targetPackage.equals(packageName) && !targetPackage.startsWith("java.") &&
                !targetPackage.startsWith("javax.") && !targetPackage.startsWith("org.slf4j") &&
                !targetPackage.startsWith("org.apache.log4j")) {
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
            if (!classes.isEmpty()) map.put("classes", classes);
            if (!dependencies.isEmpty()) map.put("dependencies", dependencies);
            return map;
        }
    }

    /**
     * Build package structure using JavaParser AST (accurate, not regex-based).
     */
    public PackageNode build(Path projectRoot) throws IOException {
        Map<String, PackageNode> allPackages = new LinkedHashMap<>();

        Files.walk(projectRoot)
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> p.toString().contains("src"))
                .filter(p -> !p.toString().contains("test") && !p.toString().contains("target"))
                .forEach(path -> {
                    try {
                        CompilationUnit cu = StaticJavaParser.parse(path);
                        String packageName = cu.getPackageDeclaration()
                                .map(pd -> pd.getNameAsString()).orElse("");
                        String filePath = projectRoot.relativize(path).toString();

                        // Find all type declarations
                        List<String> classNames = new ArrayList<>();
                        for (ClassOrInterfaceDeclaration c : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                            classNames.add(c.getNameAsString());
                        }
                        for (EnumDeclaration e : cu.findAll(EnumDeclaration.class)) {
                            classNames.add(e.getNameAsString());
                        }

                        // Add to package tree
                        String rootPkg = packageName.isEmpty() ? "(default)" : packageName.split("\\.")[0];
                        PackageNode root = allPackages.computeIfAbsent(rootPkg, PackageNode::new);

                        if (packageName.isEmpty()) {
                            root.addClasses(classNames);
                        } else {
                            String[] parts = packageName.split("\\.");
                            PackageNode current = root;
                            for (int i = 1; i < parts.length; i++) {
                                current = current.getOrCreateSubPackage(parts[i]);
                            }
                            current.addClasses(classNames);
                        }

                        // Extract dependencies from imports
                        cu.getImports().forEach(importDecl -> {
                            String impPkg = importDecl.getNameAsString();
                            int lastDot = impPkg.lastIndexOf('.');
                            if (lastDot > 0) {
                                String pkg = impPkg.substring(0, lastDot);
                                if (!packageName.isEmpty()) {
                                    root.addDependency(pkg);
                                }
                            }
                        });
                    } catch (Exception e) {
                        // ignore parse errors
                    }
                });

        // If only one root package, return it; otherwise merge
        if (allPackages.size() == 1) {
            return allPackages.values().iterator().next();
        }

        // Merge into a synthetic root
        PackageNode merged = new PackageNode("(project root)");
        for (PackageNode node : allPackages.values()) {
            merged.getSubPackages().put(node.getPackageName(), node);
            merged.getDependencies().addAll(node.getDependencies());
        }
        return merged;
    }

    public static String inferLayer(String packageName) {
        String lower = packageName.toLowerCase();
        if (lower.contains(".controller") || lower.contains(".web") || lower.contains(".rest") || lower.contains(".api"))
            return "CONTROLLER";
        if (lower.contains(".service") || lower.contains(".biz"))
            return "SERVICE";
        if (lower.contains(".repository") || lower.contains(".mapper") || lower.contains(".dao") ||
            lower.contains(".jpa") || lower.contains(".jdbc"))
            return "REPOSITORY";
        if (lower.contains(".entity") || lower.contains(".model") || lower.contains(".pojo") ||
            lower.contains(".bean") || lower.contains(".domain") || lower.endsWith(".po"))
            return "ENTITY";
        if (lower.contains(".dto") || lower.contains(".vo") || lower.contains(".request") ||
            lower.contains(".response") || lower.contains(".command") || lower.contains(".query"))
            return "DTO";
        if (lower.contains(".config") || lower.contains(".configuration"))
            return "CONFIG";
        if (lower.contains(".util") || lower.contains(".common") || lower.contains(".helper") || lower.contains(".metrics"))
            return "UTIL";
        if (lower.contains(".listener") || lower.contains(".consumer") || lower.contains(".handler"))
            return "HANDLER";
        if (lower.contains(".job") || lower.contains(".task") || lower.contains(".schedule"))
            return "SCHEDULED";
        if (lower.contains(".filter") || lower.contains(".interceptor") || lower.contains(".aspect"))
            return "MIDDLEWARE";
        if (lower.contains(".exception") || lower.contains(".error"))
            return "EXCEPTION";
        if (lower.contains(".pool") || lower.contains(".core") || lower.contains(".internal") ||
            lower.contains(".impl") || lower.contains(".proxy"))
            return "CORE";
        return "OTHER";
    }

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

    public Map<String, Object> export(PackageNode root) {
        return root.toMap();
    }

    public Map<String, Integer> summarizeLayers(PackageNode root) {
        Map<String, Integer> layers = new LinkedHashMap<>();
        countLayers(root, layers);
        return layers;
    }

    private void countLayers(PackageNode node, Map<String, Integer> layers) {
        String layer = node.getLayer();
        if (!layer.isEmpty()) layers.merge(layer, node.getTotalClassCount(), Integer::sum);
        for (PackageNode sub : node.getSubPackages().values()) countLayers(sub, layers);
    }
}
