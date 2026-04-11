package cn.dolphinmind.glossary.java.analyze.core;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core Feature 4: Type Definition Navigator (JavaParser-based)
 *
 * Indexes all types (classes, interfaces, enums) in the project
 * and allows navigating from any type reference to its definition file.
 */
public class TypeDefinitionNavigator {

    private final Map<String, String> typeToFile = new LinkedHashMap<>();
    private final Map<String, String> typeToPackage = new LinkedHashMap<>();
    private final Map<String, List<String>> typeToMethods = new LinkedHashMap<>();
    private int totalTypes = 0;

    public void buildIndex(Path projectRoot) throws IOException {
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

                        for (ClassOrInterfaceDeclaration c : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                            registerType(c.getNameAsString(), packageName, filePath, c.getMethods());
                        }
                        for (EnumDeclaration e : cu.findAll(EnumDeclaration.class)) {
                            registerType(e.getNameAsString(), packageName, filePath, Collections.emptyList());
                        }
                    } catch (Exception e) {}
                });
    }

    private void registerType(String name, String pkg, String file, List<MethodDeclaration> methods) {
        String full = (pkg.isEmpty() ? "" : pkg + ".") + name;
        typeToFile.put(full, file);
        typeToFile.put(name, file);
        typeToPackage.put(full, pkg);
        typeToPackage.put(name, pkg);

        List<String> methodNames = methods.stream()
                .map(MethodDeclaration::getNameAsString)
                .collect(Collectors.toList());
        typeToMethods.put(full, methodNames);
        typeToMethods.put(name, methodNames);
        totalTypes++;
    }

    public String navigateTo(String typeName) {
        String file = typeToFile.get(typeName);
        if (file != null) return file;

        // Try simple name
        int lastDot = typeName.lastIndexOf('.');
        if (lastDot > 0) {
            String simple = typeName.substring(lastDot + 1);
            file = typeToFile.get(simple);
            if (file != null) return file;
        }

        // Try removing generics
        int genIdx = typeName.indexOf('<');
        if (genIdx > 0) return navigateTo(typeName.substring(0, genIdx));

        // Try package suffix matching
        for (Map.Entry<String, String> entry : typeToFile.entrySet()) {
            if (entry.getKey().endsWith("." + typeName) || entry.getKey().equals(typeName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public List<String> getMethods(String typeName) {
        return typeToMethods.getOrDefault(typeName, Collections.emptyList());
    }

    public String getPackage(String typeName) {
        return typeToPackage.getOrDefault(typeName, "");
    }

    public int getTotalTypes() { return totalTypes; }

    public void printSummary() {
        System.out.println("\n=== 类型定义索引 (" + totalTypes + " 个类型) ===");
        Map<String, List<String>> pkgTypes = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : typeToPackage.entrySet()) {
            if (!entry.getKey().contains(".")) continue;
            pkgTypes.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(entry.getKey());
        }

        for (Map.Entry<String, List<String>> entry : pkgTypes.entrySet()) {
            System.out.println("\n  " + (entry.getKey().isEmpty() ? "(default)" : entry.getKey()) +
                    " (" + entry.getValue().size() + " types)");
            int shown = 0;
            for (String type : entry.getValue()) {
                if (shown >= 5) {
                    System.out.println("    ... +" + (entry.getValue().size() - 5));
                    break;
                }
                System.out.println("    " + type + " [" + typeToMethods.getOrDefault(type, Collections.emptyList()).size() + " methods]");
                shown++;
            }
        }
    }

    public Map<String, Object> export() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("total_types", totalTypes);
        Map<String, Map<String, Object>> types = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : typeToMethods.entrySet()) {
            if (!entry.getKey().contains(".")) continue;
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("file", typeToFile.getOrDefault(entry.getKey(), ""));
            info.put("package", typeToPackage.getOrDefault(entry.getKey(), ""));
            info.put("method_count", entry.getValue().size());
            types.put(entry.getKey(), info);
        }
        map.put("types", types);
        return map;
    }

    public int getIndexSize() { return totalTypes; }
}
