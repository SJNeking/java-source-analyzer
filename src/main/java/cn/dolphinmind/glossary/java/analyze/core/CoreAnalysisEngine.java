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
    private final CallChainTracer callChainTracer = new CallChainTracer();
    private final BytecodeCallGraphBuilder bytecodeCallGraphBuilder = new BytecodeCallGraphBuilder();
    private final PackageStructureMapper packageMapper = new PackageStructureMapper();
    private final TypeDefinitionNavigator typeNavigator = new TypeDefinitionNavigator();
    private final DataFlowTracer dataFlowTracer = new DataFlowTracer();

    public Map<String, Object> analyze(Path projectRoot) throws IOException {
        System.out.println("\n=== 核心分析引擎启动 ===");

        // 1. Package Structure (JavaParser-based)
        System.out.println("📦 正在分析包结构...");
        PackageStructureMapper.PackageNode packageTree = packageMapper.build(projectRoot);

        // 2. Entry Points (JavaParser-based)
        System.out.println("🔍 正在发现入口点...");
        List<EntryPointDiscovery.EntryPoint> entryPoints = entryPointDiscovery.discover(projectRoot);

        // 3. Type Definition Index (JavaParser-based)
        System.out.println("📚 正在构建类型定义索引...");
        typeNavigator.buildIndex(projectRoot);

        // 4. Call Graph: Bytecode analysis (primary) + Source analysis (fallback)
        System.out.println("🔗 正在构建调用图...");

        // Try bytecode analysis first (most accurate)
        BytecodeCallGraphBuilder.BytecodeCallGraph bytecodeGraph = null;
        try {
            bytecodeGraph = bytecodeCallGraphBuilder.buildFromBytecode(projectRoot);
            if (bytecodeGraph.getInternalCalls() > 0) {
                System.out.println("  ✅ 字节码分析: " + bytecodeGraph.getInternalCalls() + " 内部调用, " +
                        bytecodeGraph.getExternalCalls() + " 外部调用已跳过");
            }
        } catch (Exception e) {
            System.out.println("  ⚠️ 字节码分析不可用: " + e.getMessage());
        }

        // Fallback to source-based analysis if bytecode not available
        CallChainTracer.CallGraph callGraph = callChainTracer.buildCallGraph(projectRoot);

        // If bytecode analysis succeeded, prefer its results
        if (bytecodeGraph != null && bytecodeGraph.getInternalCalls() > 0) {
            // Merge bytecode results into callGraph for tracing
            for (Map.Entry<String, Set<String>> entry : bytecodeGraph.getAdjacencyList().entrySet()) {
                for (String callee : entry.getValue()) {
                    callGraph.addCall(entry.getKey(), callee, true);
                }
            }
        }

        // Trace chains from entry points
        List<String> entryPointKeys = new ArrayList<>();
        for (EntryPointDiscovery.EntryPoint ep : entryPoints) {
            entryPointKeys.add(ep.getFullClassName() + "#" + ep.getMethodName());
        }
        Map<String, List<CallChainTracer.CallChain>> callChains =
                callChainTracer.traceAll(callGraph, entryPointKeys, 5);

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
        callInfo.put("node_count", callGraph.getNodeCount());
        callInfo.put("edge_count", callGraph.getEdgeCount());
        callInfo.put("resolved_internal", callGraph.getResolvedInternal());
        callInfo.put("unresolved_fallback", callGraph.getUnresolvedFallback());
        callInfo.put("skipped_external", callGraph.getSkippedExternal());
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
        printSummary(entryPoints, packageTree, callChains, callGraph, dataFlows);

        return result;
    }

    public void printSummary(List<EntryPointDiscovery.EntryPoint> entryPoints,
                              PackageStructureMapper.PackageNode packageTree,
                              Map<String, List<CallChainTracer.CallChain>> callChains,
                              CallChainTracer.CallGraph callGraph,
                              List<DataFlowTracer.DataFlow> dataFlows) {
        // Entry points
        System.out.println("\n=== 项目解构报告 ===");
        entryPointDiscovery.printSummary(entryPoints);

        // Package structure
        System.out.println("\n📦 包结构:");
        packageMapper.printTree(packageTree);

        // Call chains
        callChainTracer.printChains(callChains, callGraph);

        // Type definitions
        System.out.println("\n📚 类型定义: 已索引 " + typeNavigator.getTotalTypes() + " 个类型");

        // Data flows
        if (!dataFlows.isEmpty()) {
            dataFlowTracer.printFlows(dataFlows);
        }
    }
}
