package cn.dolphinmind.glossary.redisson;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.google.gson.GsonBuilder;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author mingxilv
 * @date 2026/4/4
 * @description Redisson 源码全量数字化引擎 - 整合 S1-S7
 */
public class ConstructorTest {

    private static final String SRC_ROOT = "/Users/mingxilv/WebDevelopment/gitcode/dev-proj/framework_source/redisson/redisson/src/main/java/org/redisson";
    private static final String OUTPUT_PATH = "src/test/resources/Redisson_Step7_Matrix.json";

    @Test
    public void executeMasterScan() throws Exception {
        List<Map<String, Object>> masterMatrix = new ArrayList<>();

        Files.walk(Paths.get(SRC_ROOT))
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(path -> {
                    try {
                        CompilationUnit cu = StaticJavaParser.parse(path);
                        String pkg = cu.getPackageDeclaration().map(pd -> pd.getNameAsString()).orElse("default");

                        Map<String, String> importMap = cu.getImports().stream()
                                .collect(Collectors.toMap(im -> im.getName().getIdentifier(), im -> im.getNameAsString(), (e, r) -> e));

                        cu.getTypes().forEach(type -> {
                            Map<String, Object> node = new LinkedHashMap<>();
                            List<String> tpList = getTypeParameters(type);

                            // --- [S1-S2: 身份与身份标识] ---
                            node.put("address", pkg + "." + type.getNameAsString());
                            node.put("kind", resolveKind(type));
                            node.put("generics", tpList);
                            node.put("modifiers", resolveFullModifiers(type));

                            // --- [S3-S4: 全路径血缘] ---
                            if (type instanceof ClassOrInterfaceDeclaration) {
                                node.put("hierarchy", resolveHierarchy((ClassOrInterfaceDeclaration) type, importMap, pkg, tpList));
                            }

                            // --- [S5: 异常风险契约] ---
                            node.put("external_throws", resolveExternalThrows(type, importMap, pkg, tpList));

                            // --- [S6: 内部零件(Fields)] ---
                            node.put("fields", resolveFields(type, importMap, pkg, tpList));

                            // --- [🚀 S7: 构造方法契约 - 核心新增] ---
                            node.put("constructors", resolveConstructors(type, importMap, pkg, tpList));

                            masterMatrix.add(node);
                        });
                    } catch (Exception ignored) {}
                });

        saveMatrix(masterMatrix);
        System.out.println("🚀 S1-S7 数字化底座构建完成！节点总数: " + masterMatrix.size());
    }

    /**
     * S7 逻辑：递归解析构造函数及其入参全路径
     */
    /**
     * S7 深度拆解：构造方法契约
     * 不仅解析类型，还保留了参数顺序和修饰符，用于后续分析组件注入链路
     */
    private List<Map<String, Object>> resolveConstructors(TypeDeclaration<?> t, Map<String, String> importMap, String pkg, List<String> tpList) {
        List<Map<String, Object>> constructorList = new ArrayList<>();

        t.getConstructors().forEach(constructor -> {
            Map<String, Object> cNode = new LinkedHashMap<>();

            // 1. 访问控制：识别是 public 接口还是 internal 组装
            cNode.put("modifiers", constructor.getModifiers().stream()
                    .map(m -> m.getKeyword().asString()).collect(Collectors.toList()));

            // 2. 依赖矩阵：解析每一个入参的身份
            List<Map<String, String>> params = constructor.getParameters().stream().map(p -> {
                Map<String, String> pMap = new LinkedHashMap<>();
                pMap.put("name", p.getNameAsString());
                // 🚀 核心：调用递归解析器，处理如 RObject, List<V> 等复杂类型
                pMap.put("type", resolveRecursiveType(p.getType(), importMap, pkg, tpList));
                return pMap;
            }).collect(Collectors.toList());

            cNode.put("parameters", params);

            // 3. 异常抛出：构造过程中可能触发的初始化失败
            cNode.put("throws", constructor.getThrownExceptions().stream()
                    .map(te -> resolveBaseName(te.asString(), importMap, pkg))
                    .collect(Collectors.toList()));

            constructorList.add(cNode);
        });

        return constructorList;
    }

    // ======================== 解析引擎工具库 (递归、符号感知) ========================

    private String resolveRecursiveType(Type type, Map<String, String> importMap, String pkg, List<String> tpList) {
        if (type instanceof ClassOrInterfaceType) {
            ClassOrInterfaceType cit = (ClassOrInterfaceType) type;
            String baseName = cit.getNameAsString();
            if (tpList.contains(baseName)) return baseName; // 符号感知

            String qualifiedBase = resolveBaseName(baseName, importMap, pkg);
            if (cit.getTypeArguments().isPresent()) {
                String args = cit.getTypeArguments().get().stream()
                        .map(arg -> resolveRecursiveType(arg, importMap, pkg, tpList))
                        .collect(Collectors.joining(", "));
                return qualifiedBase + "<" + args + ">";
            }
            return qualifiedBase;
        }
        return type.asString();
    }

    private String resolveBaseName(String name, Map<String, String> importMap, String pkg) {
        if (importMap.containsKey(name)) return importMap.get(name);
        List<String> javaTypes = Arrays.asList("String", "Integer", "Long", "Boolean", "Object", "List", "Map", "Set", "Collection", "Exception", "RFuture");
        if (javaTypes.contains(name)) {
            if (name.startsWith("R") && !name.equals("Runnable")) return "org.redisson.api." + name;
            if (Arrays.asList("List", "Map", "Set", "Collection").contains(name)) return "java.util." + name;
            return "java.lang." + name;
        }
        return pkg + "." + name;
    }

    private List<Map<String, Object>> resolveFields(TypeDeclaration<?> t, Map<String, String> importMap, String pkg, List<String> tpList) {
        List<Map<String, Object>> fields = new ArrayList<>();
        t.getFields().forEach(f -> {
            List<String> mods = f.getModifiers().stream().map(m -> m.getKeyword().asString()).collect(Collectors.toList());
            f.getVariables().forEach(v -> {
                Map<String, Object> fNode = new LinkedHashMap<>();
                fNode.put("name", v.getNameAsString());
                fNode.put("type", resolveRecursiveType(v.getType(), importMap, pkg, tpList));
                fNode.put("modifiers", mods);
                fields.add(fNode);
            });
        });
        return fields;
    }

    private Map<String, List<String>> resolveHierarchy(ClassOrInterfaceDeclaration cid, Map<String, String> importMap, String pkg, List<String> tpList) {
        Map<String, List<String>> h = new LinkedHashMap<>();
        h.put("extends", cid.getExtendedTypes().stream().map(t -> resolveRecursiveType(t, importMap, pkg, tpList)).collect(Collectors.toList()));
        h.put("implements", cid.getImplementedTypes().stream().map(t -> resolveRecursiveType(t, importMap, pkg, tpList)).collect(Collectors.toList()));
        return h;
    }

    private List<String> resolveExternalThrows(TypeDeclaration<?> t, Map<String, String> importMap, String pkg, List<String> tpList) {
        Set<String> ex = new HashSet<>();
        t.getMethods().forEach(m -> m.getThrownExceptions().forEach(te -> {
            if (!tpList.contains(te.asString())) {
                String full = resolveBaseName(te.asString(), importMap, pkg);
                if (!full.startsWith("org.redisson")) ex.add(full);
            }
        }));
        return new ArrayList<>(ex);
    }

    private List<String> resolveFullModifiers(TypeDeclaration<?> t) {
        List<String> mods = t.getModifiers().stream().map(m -> m.getKeyword().asString()).collect(Collectors.toList());
        if (mods.isEmpty()) mods.add("package-private");
        return mods;
    }

    private String resolveKind(TypeDeclaration<?> t) {
        if (t instanceof ClassOrInterfaceDeclaration) {
            ClassOrInterfaceDeclaration cid = (ClassOrInterfaceDeclaration) t;
            return cid.isInterface() ? "INTERFACE" : (cid.isAbstract() ? "ABSTRACT_CLASS" : "CLASS");
        }
        return t.isEnumDeclaration() ? "ENUM" : (t.isAnnotationDeclaration() ? "ANNOTATION" : "UNKNOWN");
    }

    private List<String> getTypeParameters(TypeDeclaration<?> t) {
        if (t instanceof ClassOrInterfaceDeclaration) {
            return ((ClassOrInterfaceDeclaration) t).getTypeParameters().stream().map(tp -> tp.asString()).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private void saveMatrix(Object data) throws Exception {
        new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(data, new FileWriter(new File(OUTPUT_PATH)));
    }
}