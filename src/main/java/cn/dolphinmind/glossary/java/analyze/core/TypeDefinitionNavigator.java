package cn.dolphinmind.glossary.java.analyze.core;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * Core Feature 4: Type Definition Navigator
 *
 * For any class/method reference, find its definition file.
 * This is the "Go to Definition" feature for understanding any Java project.
 *
 * Usage:
 * - "userService.save()" → find where UserService is defined
 * - "OrderRepository.findById()" → find OrderRepository.java
 * - Given any import, find the actual file
 */
public class TypeDefinitionNavigator {

    /**
     * Maps type names to their definition files.
     */
    private final Map<String, String> typeToFile = new LinkedHashMap<>();
    private final Map<String, String> typeToPackage = new LinkedHashMap<>();
    private final Map<String, List<String>> typeToMethods = new LinkedHashMap<>();

    /**
     * Build the type index for a project.
     */
    public void buildIndex(Path projectRoot) throws IOException {
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
                        String filePath = projectRoot.relativize(path).toString();

                        typeToFile.put(className, filePath);
                        typeToFile.put(fullClassName, filePath);
                        typeToPackage.put(className, packageName);
                        typeToPackage.put(fullClassName, packageName);

                        // Extract method signatures
                        List<String> methods = extractMethodSignatures(content);
                        typeToMethods.put(fullClassName, methods);
                        typeToMethods.put(className, methods);
                    } catch (Exception e) {
                        // ignore
                    }
                });
    }

    /**
     * Navigate to the definition of a type.
     * Returns the file path or null if not found.
     */
    public String navigateTo(String typeName) {
        // Try exact match
        String file = typeToFile.get(typeName);
        if (file != null) return file;

        // Try simple name
        int lastDot = typeName.lastIndexOf('.');
        if (lastDot > 0) {
            String simpleName = typeName.substring(lastDot + 1);
            file = typeToFile.get(simpleName);
            if (file != null) return file;
        }

        // Try removing generics
        int genericIdx = typeName.indexOf('<');
        if (genericIdx > 0) {
            String rawType = typeName.substring(0, genericIdx);
            return navigateTo(rawType);
        }

        // Try package prefix matching
        for (Map.Entry<String, String> entry : typeToFile.entrySet()) {
            if (entry.getKey().endsWith("." + typeName) || entry.getKey().equals(typeName)) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Get all methods of a type.
     */
    public List<String> getMethods(String typeName) {
        return typeToMethods.getOrDefault(typeName, Collections.emptyList());
    }

    /**
     * Get the package of a type.
     */
    public String getPackage(String typeName) {
        return typeToPackage.getOrDefault(typeName, "");
    }

    /**
     * Print the type index summary.
     */
    public void printSummary() {
        System.out.println("\n=== 类型定义导航索引 ===");
        System.out.println("共索引 " + typeToFile.size() + " 个类型引用, " +
                typeToMethods.size() + " 个类型定义");

        // Group by package
        Map<String, List<String>> pkgTypes = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : typeToPackage.entrySet()) {
            pkgTypes.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(entry.getKey());
        }

        for (Map.Entry<String, List<String>> entry : pkgTypes.entrySet()) {
            System.out.println("\n  " + (entry.getKey().isEmpty() ? "(default package)" : entry.getKey()) +
                    " (" + entry.getValue().size() + " types)");
            int shown = 0;
            for (String type : entry.getValue()) {
                if (shown >= 5) {
                    System.out.println("    ... +" + (entry.getValue().size() - 5));
                    break;
                }
                System.out.println("    " + type);
                shown++;
            }
        }
    }

    /**
     * Export as JSON-compatible map.
     */
    public Map<String, Object> export() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("total_types", typeToMethods.size());
        Map<String, Map<String, Object>> types = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : typeToMethods.entrySet()) {
            if (!entry.getKey().contains(".")) continue; // skip simple names
            Map<String, Object> typeInfo = new LinkedHashMap<>();
            typeInfo.put("file", typeToFile.getOrDefault(entry.getKey(), ""));
            typeInfo.put("package", typeToPackage.getOrDefault(entry.getKey(), ""));
            typeInfo.put("method_count", entry.getValue().size());
            types.put(entry.getKey(), typeInfo);
        }
        map.put("types", types);
        return map;
    }

    // ---- Private helpers ----

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

    private List<String> extractMethodSignatures(String content) {
        List<String> methods = new ArrayList<>();
        String[] lines = content.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if ((trimmed.contains("public ") || trimmed.contains("private ") || trimmed.contains("protected ")) &&
                trimmed.contains("(") && trimmed.contains(")") && !trimmed.startsWith("//") && !trimmed.startsWith("/*")) {
                // Extract method name
                int parenIdx = trimmed.indexOf('(');
                if (parenIdx > 0) {
                    String beforeParen = trimmed.substring(0, parenIdx).trim();
                    String[] parts = beforeParen.split("\\s+");
                    if (parts.length >= 2) {
                        methods.add(parts[parts.length - 1]);
                    }
                }
            }
        }
        return methods;
    }
}
