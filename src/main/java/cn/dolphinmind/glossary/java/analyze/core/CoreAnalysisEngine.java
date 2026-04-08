package cn.dolphinmind.glossary.java.analyze.core;

import com.github.javaparser.ast.body.MethodDeclaration;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * Core Analysis Engine - ties together the 5 core features.
 *
 * All features use JavaParser AST (not regex/string parsing).
 */
public class CoreAnalysisEngine {

    private final EntryPointDiscovery entryPointDiscovery = new EntryPointDiscovery();
    private CallChainTracer callChainTracer;
    private final BytecodeCallGraphBuilder bytecodeCallGraphBuilder = new BytecodeCallGraphBuilder();
    private final PackageStructureMapper packageMapper = new PackageStructureMapper();
    private final TypeDefinitionNavigator typeNavigator = new TypeDefinitionNavigator();
    private final DataFlowTracer dataFlowTracer = new DataFlowTracer();
    private ExternalLibrarySignatureDB signatureDB;

    public ExternalLibrarySignatureDB getSignatureDB() { return signatureDB; }

    public Map<String, Object> analyze(Path projectRoot) throws IOException {
        System.out.println("\n=== 核心分析引擎启动 ===");

        // 0. Build external library method signature database
        System.out.println("📚 正在构建外部库方法签名数据库...");
        signatureDB = new ExternalLibrarySignatureDB();
        try {
            signatureDB.build(projectRoot);
            System.out.println("  ✅ 已索引: " + signatureDB.getJarsScanned() + " 个jar, " +
                    signatureDB.getClassesScanned() + " 个类, " +
                    signatureDB.getMethodsIndexed() + " 个方法签名");
        } catch (Exception e) {
            System.out.println("  ⚠️ 外部库签名构建失败: " + e.getMessage());
        }

        // Initialize call chain tracer with signature DB
        callChainTracer = new CallChainTracer(signatureDB);

        // 1. Package Structure (JavaParser-based)
        System.out.println("📦 正在分析包结构...");
        PackageStructureMapper.PackageNode packageTree = packageMapper.build(projectRoot);

        // 2. Entry Points (JavaParser-based)
        System.out.println("🔍 正在发现入口点...");
        List<EntryPointDiscovery.EntryPoint> entryPoints = entryPointDiscovery.discover(projectRoot);

        // 3. Type Definition Index (JavaParser-based)
        System.out.println("📚 正在构建类型定义索引...");
        typeNavigator.buildIndex(projectRoot);

        // 4. Call Graph: Progressive analysis (source → bytecode)
        System.out.println("🔗 正在构建调用图...");

        // Layer 1: Source analysis (semantic understanding)
        System.out.println("  📖 第一层: 源码分析...");
        CallChainTracer.CallGraph sourceGraph = callChainTracer.buildCallGraph(projectRoot);
        System.out.println("     解析: " + sourceGraph.getResolvedInternal() + " 内部调用, " +
                sourceGraph.getUnresolvedFallback() + " 回退(待字节码补充), " +
                sourceGraph.getSkippedExternal() + " 外部库已跳过");

        // Layer 2: Bytecode analysis (exact call targets, fills source gaps)
        System.out.println("  ⚡ 第二层: 字节码分析...");
        BytecodeCallGraphBuilder.BytecodeCallGraph bytecodeGraph = null;
        try {
            bytecodeGraph = bytecodeCallGraphBuilder.buildFromBytecode(projectRoot);
            if (bytecodeGraph.getInternalCalls() > 0) {
                System.out.println("     解析: " + bytecodeGraph.getInternalCalls() + " 内部调用(0 回退), " +
                        bytecodeGraph.getExternalCalls() + " 外部库已跳过");
            }
        } catch (Exception e) {
            System.out.println("     ⚠️ 字节码分析不可用: " + e.getMessage());
        }

        // Merge: source graph enriched with bytecode accuracy
        // For calls where source analysis had fallback, replace with bytecode target
        CallChainTracer.CallGraph mergedGraph = sourceGraph;
        if (bytecodeGraph != null && bytecodeGraph.getInternalCalls() > 0) {
            int enriched = 0;
            for (Map.Entry<String, Set<String>> entry : bytecodeGraph.getAdjacencyList().entrySet()) {
                for (String callee : entry.getValue()) {
                    mergedGraph.addCall(entry.getKey(), callee, true);
                    enriched++;
                }
            }
            System.out.println("  📊 渐进合并: 源码语义 + 字节码精度 (" + enriched + " 调用已补充)");
        }

        // Trace chains from entry points using merged graph
        List<String> entryPointKeys = new ArrayList<>();
        for (EntryPointDiscovery.EntryPoint ep : entryPoints) {
            entryPointKeys.add(ep.getFullClassName() + "#" + ep.getMethodName());
        }
        Map<String, List<CallChainTracer.CallChain>> callChains =
                callChainTracer.traceAll(mergedGraph, entryPointKeys, 5);

        // 5. Data Flow (JavaParser-based, limited to first few entry points)
        System.out.println("🌊 正在追踪数据流...");
        Map<String, MethodDeclaration> methodIndex = dataFlowTracer.indexMethods(projectRoot);
        List<DataFlowTracer.DataFlow> dataFlows = new ArrayList<>();
        int flowCount = 0;
        for (EntryPointDiscovery.EntryPoint ep : entryPoints) {
            if (flowCount >= 5) break;
            try {
                List<DataFlowTracer.DataFlow> epFlows = dataFlowTracer.traceMethod(
                        methodIndex, ep.getFullClassName(), ep.getMethodName());
                dataFlows.addAll(epFlows);
                flowCount++;
            } catch (Exception e) {}
        }

        // Build result
        Map<String, Object> result = new LinkedHashMap<>();

        // Package structure
        Map<String, Object> pkgInfo = new LinkedHashMap<>();
        pkgInfo.put("layer_summary", packageMapper.summarizeLayers(packageTree));
        pkgInfo.put("tree", packageMapper.export(packageTree));
        result.put("package_structure", pkgInfo);

        // Entry points
        Map<String, Object> entryInfo = new LinkedHashMap<>();
        entryInfo.put("total", entryPoints.size());
        entryInfo.put("by_type", entryPointDiscovery.groupByType(entryPoints).entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        e -> e.getKey().name(),
                        e -> e.getValue().stream().map(EntryPointDiscovery.EntryPoint::toMap)
                                .collect(java.util.stream.Collectors.toList()))));
        result.put("entry_points", entryInfo);

        // Call graph
        Map<String, Object> callInfo = new LinkedHashMap<>();
        callInfo.put("analysis_layers", bytecodeGraph != null ? 2 : 1);
        callInfo.put("source_resolved", sourceGraph.getResolvedInternal());
        callInfo.put("source_fallback", sourceGraph.getUnresolvedFallback());
        callInfo.put("source_skipped_external", sourceGraph.getSkippedExternal());
        callInfo.put("node_count", mergedGraph.getNodeCount());
        callInfo.put("edge_count", mergedGraph.getEdgeCount());
        callInfo.put("resolved_internal", mergedGraph.getResolvedInternal());
        callInfo.put("unresolved_fallback", mergedGraph.getUnresolvedFallback());
        callInfo.put("skipped_external", mergedGraph.getSkippedExternal());
        if (bytecodeGraph != null) {
            callInfo.put("bytecode_total", bytecodeGraph.getTotalCalls());
            callInfo.put("bytecode_internal", bytecodeGraph.getInternalCalls());
            callInfo.put("bytecode_external", bytecodeGraph.getExternalCalls());
        }
        Map<String, Object> chainsOutput = new LinkedHashMap<>();
        for (Map.Entry<String, List<CallChainTracer.CallChain>> e : callChains.entrySet()) {
            chainsOutput.put(e.getKey(), e.getValue().stream().limit(10)
                    .map(CallChainTracer.CallChain::toMap).collect(java.util.stream.Collectors.toList()));
        }
        callInfo.put("chains_from_entry", chainsOutput);
        result.put("call_graph", callInfo);

        // Type definitions
        result.put("type_definitions", typeNavigator.export());

        // Data flows
        result.put("data_flows", dataFlowTracer.export(dataFlows));

        // Print summary
        printSummary(entryPoints, packageTree, callChains, mergedGraph, bytecodeGraph, dataFlows);

        return result;
    }

    public void printSummary(List<EntryPointDiscovery.EntryPoint> entryPoints,
                              PackageStructureMapper.PackageNode packageTree,
                              Map<String, List<CallChainTracer.CallChain>> callChains,
                              CallChainTracer.CallGraph mergedGraph,
                              BytecodeCallGraphBuilder.BytecodeCallGraph bytecodeGraph,
                              List<DataFlowTracer.DataFlow> dataFlows) {
        // Entry points
        System.out.println("\n=== 项目解构报告 ===");
        entryPointDiscovery.printSummary(entryPoints);

        // Package structure
        System.out.println("\n📦 包结构:");
        packageMapper.printTree(packageTree);

        // Call chains
        callChainTracer.printChains(callChains, mergedGraph);

        // Type definitions
        System.out.println("\n📚 类型定义: 已索引 " + typeNavigator.getTotalTypes() + " 个类型");

        // Data flows
        if (!dataFlows.isEmpty()) {
            dataFlowTracer.printFlows(dataFlows);
        }
    }
}
