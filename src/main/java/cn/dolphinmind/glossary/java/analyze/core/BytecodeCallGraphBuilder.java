package cn.dolphinmind.glossary.java.analyze.core;

import org.objectweb.asm.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;

/**
 * Core Feature 2 (Improved): Bytecode-based Call Graph Builder
 *
 * Uses ASM library to analyze compiled .class files for accurate call chains.
 * Advantages over source-based analysis:
 * 1. No generic erasure issues - bytecode has exact types
 * 2. No complex expression parsing - bytecode shows exact method targets
 * 3. No symbol resolution failures - bytecode INVOKEVIRTUAL directly tells us the target class
 *
 * Only tracks calls between INTERNAL project classes (within target/classes).
 */
public class BytecodeCallGraphBuilder {

    public static class BytecodeCallGraph {
        // callerClass#callerMethod → Set of (calleeClass#calleeMethod)
        private final Map<String, Set<String>> adjacencyList = new LinkedHashMap<>();
        // All known internal class names
        private final Set<String> internalClasses = new HashSet<>();
        private int totalCalls = 0;
        private int internalCalls = 0;
        private int externalCalls = 0;

        public void registerClass(String fullClassName) {
            internalClasses.add(fullClassName);
            int lastDot = fullClassName.lastIndexOf('.');
            if (lastDot > 0) internalClasses.add(fullClassName.substring(lastDot + 1));
        }

        public void addCall(String caller, String callee, boolean isInternal) {
            totalCalls++;
            if (isInternal) {
                internalCalls++;
                adjacencyList.computeIfAbsent(caller, k -> new LinkedHashSet<>()).add(callee);
            } else {
                externalCalls++;
            }
        }

        public Map<String, Set<String>> getAdjacencyList() { return Collections.unmodifiableMap(adjacencyList); }
        public Set<String> getInternalClasses() { return Collections.unmodifiableSet(internalClasses); }
        public int getNodeCount() { return adjacencyList.size(); }
        public int getEdgeCount() { return adjacencyList.values().stream().mapToInt(Set::size).sum(); }
        public int getTotalCalls() { return totalCalls; }
        public int getInternalCalls() { return internalCalls; }
        public int getExternalCalls() { return externalCalls; }
    }

    /**
     * Build call graph from compiled .class files.
     * Uses ASM ClassReader to parse bytecode and extract exact method call targets.
     */
    public BytecodeCallGraph buildFromBytecode(Path projectRoot) throws IOException {
        BytecodeCallGraph graph = new BytecodeCallGraph();

        // Find target/classes directory
        final Path classesDirRef;
        Path classesDir = projectRoot.resolve("target/classes");
        if (!Files.exists(classesDir)) {
            // Try multi-module: scan for target/classes in subdirectories
            classesDir = findClassesDir(projectRoot);
        }
        if (classesDir == null || !Files.exists(classesDir)) {
            return graph; // No compiled classes available
        }
        classesDirRef = classesDir;

        // First pass: register all internal classes
        Files.walk(classesDirRef)
                .filter(p -> p.toString().endsWith(".class"))
                .forEach(path -> {
                    String className = pathToClassName(classesDirRef, path);
                    if (className != null) graph.registerClass(className);
                });

        // Second pass: build call graph from bytecode
        Files.walk(classesDirRef)
                .filter(p -> p.toString().endsWith(".class"))
                .forEach(path -> {
                    String className = pathToClassName(classesDirRef, path);
                    if (className == null) return;

                    try (InputStream is = Files.newInputStream(path)) {
                        ClassReader cr = new ClassReader(is);
                        ClassVisitor cv = new ClassVisitor(Opcodes.ASM9) {
                            private String currentClass;
                            private String currentMethod;

                            @Override
                            public void visit(int version, int access, String name, String signature,
                                              String superName, String[] interfaces) {
                                currentClass = name.replace('/', '.');
                            }

                            @Override
                            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                              String signature, String[] exceptions) {
                                currentMethod = currentClass + "#" + name;
                                return new MethodVisitor(Opcodes.ASM9) {
                                    @Override
                                    public void visitMethodInsn(int opcode, String owner, String name,
                                                                 String descriptor, boolean isInterface) {
                                        String calleeClass = owner.replace('/', '.');
                                        String calleeKey = calleeClass + "#" + name;

                                        boolean isInternal = graph.getInternalClasses().contains(calleeClass) ||
                                                graph.getInternalClasses().contains(calleeClass.substring(
                                                        calleeClass.lastIndexOf('.') + 1));

                                        graph.addCall(currentMethod, calleeKey, isInternal);
                                    }
                                };
                            }
                        };
                        cr.accept(cv, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                    } catch (Exception e) {
                        // ignore parse errors
                    }
                });

        return graph;
    }

    /**
     * Find target/classes directory in a multi-module project.
     */
    private Path findClassesDir(Path root) throws IOException {
        // Look for the first target/classes directory
        try {
            return Files.walk(root)
                    .filter(p -> p.toString().endsWith("target" + File.separator + "classes"))
                    .filter(Files::isDirectory)
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Convert a .class file path to a fully qualified class name.
     * e.g., target/classes/org/springframework/boot/SpringApplication.class
     *       → org.springframework.boot.SpringApplication
     */
    private String pathToClassName(Path classesDir, Path classFile) {
        try {
            String relative = classesDir.relativize(classFile).toString();
            if (!relative.endsWith(".class")) return null;
            String className = relative.substring(0, relative.length() - 6); // Remove .class
            return className.replace(File.separatorChar, '.').replace('/', '.');
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Convert BytecodeCallGraph to CallChainTracer.CallGraph for compatibility.
     */
    public Map<String, Set<String>> toAdjacencyList(BytecodeCallGraph bytecodeGraph) {
        return bytecodeGraph.getAdjacencyList();
    }

    /**
     * Print statistics about the bytecode analysis.
     */
    public void printStats(BytecodeCallGraph graph) {
        System.out.println("\n=== 字节码调用图分析 ===");
        System.out.println("  解析统计: " + graph.getInternalCalls() + " 内部调用已解析, " +
                graph.getExternalCalls() + " 外部库已跳过, " +
                graph.getTotalCalls() + " 总调用数");
        System.out.println("  节点数: " + graph.getNodeCount() + ", 边数: " + graph.getEdgeCount());
    }
}
