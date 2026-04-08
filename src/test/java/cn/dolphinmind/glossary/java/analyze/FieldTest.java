package cn.dolphinmind.glossary.java.analyze;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.google.gson.GsonBuilder;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author mingxilv
 * @date 2026/4/4
 * @description 组件内部拆解 - 修正参数不匹配报错，实现符号感知解析
 */
public class FieldTest {

    private static final String SRC_ROOT = "/Users/mingxilv/WebDevelopment/gitcode/dev-proj/framework_source/redisson/redisson/src/main/java/org/redisson";

    @Test
    @DisplayName("Step-06: 解决编译器报错 - 全量符号感知扫描")
    public void scanFullInternalUniverse() throws Exception {
        String STEP6_FILE = "Redisson_Step6_Full_Matrix.json";
        File outputFile = getTestResourceFile(STEP6_FILE);
        List<Map<String, Object>> nodeList = new ArrayList<>();

        Files.walk(Paths.get(SRC_ROOT))
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(path -> {
                    try {
                        CompilationUnit cu = StaticJavaParser.parse(path);
                        String pkg = cu.getPackageDeclaration().map(pd -> pd.getNameAsString()).orElse("default");

                        // S4: 构建地址簿
                        Map<String, String> importMap = cu.getImports().stream()
                                .collect(Collectors.toMap(im -> im.getName().getIdentifier(), im -> im.getNameAsString(), (e, r) -> e));

                        cu.getTypes().forEach(type -> {
                            Map<String, Object> node = new LinkedHashMap<>();

                            // 🚀 核心：首先提取当前组件定义的泛型符号 (K, V 等)
                            List<String> tpList = getGenerics(type);

                            node.put("address", pkg + "." + type.getNameAsString());
                            node.put("kind", getKind(type));
                            node.put("generics", tpList);
                            node.put("modifiers", getFullModifiers(type));

                            // --- 1. Hierarchy 扫描 (参数已对齐) ---
                            Map<String, List<String>> hierarchy = new LinkedHashMap<>();
                            if (type instanceof ClassOrInterfaceDeclaration) {
                                ClassOrInterfaceDeclaration cid = (ClassOrInterfaceDeclaration) type;
                                hierarchy.put("extends", cid.getExtendedTypes().stream()
                                        .map(t -> resolveRecursiveType(t, importMap, pkg, tpList)).collect(Collectors.toList()));
                                hierarchy.put("implements", cid.getImplementedTypes().stream()
                                        .map(t -> resolveRecursiveType(t, importMap, pkg, tpList)).collect(Collectors.toList()));
                            }
                            node.put("hierarchy", hierarchy);

                            // --- 2. Fields 扫描 (参数已对齐) ---
                            List<Map<String, Object>> fieldList = new ArrayList<>();
                            type.getFields().forEach(field -> {
                                List<String> mods = field.getModifiers().stream().map(m -> m.getKeyword().asString()).collect(Collectors.toList());
                                field.getVariables().forEach(var -> {
                                    Map<String, Object> fNode = new LinkedHashMap<>();
                                    fNode.put("name", var.getNameAsString());
                                    // 🚀 传入 tpList，防止 K, V 被补全包名
                                    fNode.put("type", resolveRecursiveType(var.getType(), importMap, pkg, tpList));
                                    fNode.put("modifiers", mods);
                                    fieldList.add(fNode);
                                });
                            });
                            node.put("fields", fieldList);

                            // --- 3. Throws 扫描 (外部异常契约) ---
                            Set<String> ex = new HashSet<>();
                            type.getMethods().forEach(m -> m.getThrownExceptions().forEach(te -> {
                                String typeStr = te.asString();
                                // 如果是泛型符号则忽略，否则按全路径补全
                                if (!tpList.contains(typeStr)) {
                                    String full = resolveBaseName(typeStr, importMap, pkg);
                                    if (!full.startsWith("org.redisson")) ex.add(full);
                                }
                            }));
                            node.put("method_throws_contract", new ArrayList<>(ex));

                            nodeList.add(node);
                        });
                    } catch (Exception ignored) {}
                });

        saveToJson(nodeList, outputFile);
    }

    /**
     * 修正后的递归解析器：4个参数严格匹配调用处
     */
    private String resolveRecursiveType(Type type, Map<String, String> importMap, String currentPkg, List<String> typeParameters) {
        if (type instanceof ClassOrInterfaceType) {
            ClassOrInterfaceType cit = (ClassOrInterfaceType) type;
            String baseName = cit.getNameAsString();

            // 💡 符号感知：如果是类定义的泛型占位符，直接返回符号本身
            if (typeParameters.contains(baseName)) {
                return baseName;
            }

            String qualifiedBase = resolveBaseName(baseName, importMap, currentPkg);

            if (cit.getTypeArguments().isPresent()) {
                String arguments = cit.getTypeArguments().get().stream()
                        .map(arg -> resolveRecursiveType(arg, importMap, currentPkg, typeParameters))
                        .collect(Collectors.joining(", "));
                return qualifiedBase + "<" + arguments + ">";
            }
            return qualifiedBase;
        }
        return type.asString();
    }

    private String resolveBaseName(String name, Map<String, String> importMap, String currentPkg) {
        if (importMap.containsKey(name)) return importMap.get(name);
        List<String> javaLang = Arrays.asList("String", "Integer", "Long", "Boolean", "Object", "List", "Map", "Set", "Collection", "Exception", "RFuture");
        if (javaLang.contains(name)) {
            return (name.startsWith("R") && !name.equals("Runnable")) ? "org.redisson.api." + name : "java.lang." + name;
        }
        return currentPkg + "." + name;
    }

    private List<String> getFullModifiers(TypeDeclaration<?> t) {
        List<String> mods = t.getModifiers().stream().map(m -> m.getKeyword().asString()).collect(Collectors.toList());
        if (mods.isEmpty()) mods.add("package-private");
        return mods;
    }

    private String getKind(TypeDeclaration<?> t) {
        if (t instanceof ClassOrInterfaceDeclaration) {
            ClassOrInterfaceDeclaration cid = (ClassOrInterfaceDeclaration) t;
            return cid.isInterface() ? "INTERFACE" : (cid.isAbstract() ? "ABSTRACT_CLASS" : "CLASS");
        }
        return t.isEnumDeclaration() ? "ENUM" : (t.isAnnotationDeclaration() ? "ANNOTATION" : "UNKNOWN");
    }

    private List<String> getGenerics(TypeDeclaration<?> t) {
        if (t instanceof ClassOrInterfaceDeclaration) {
            return ((ClassOrInterfaceDeclaration) t).getTypeParameters().stream().map(tp -> tp.asString()).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private void saveToJson(Object data, File file) throws Exception {
        new GsonBuilder().setPrettyPrinting().create().toJson(data, new FileWriter(file));
    }

    private File getTestResourceFile(String fileName) throws IOException {
        Path path = Paths.get("src", "test", "resources");
        if (!Files.exists(path)) Files.createDirectories(path);
        return path.resolve(fileName).toFile();
    }
}