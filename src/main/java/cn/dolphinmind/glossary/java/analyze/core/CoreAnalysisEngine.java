package cn.dolphinmind.glossary.java.analyze.core;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * Core Analysis Engine
 *
 * Ties together the 5 core features for understanding any Java project:
 * 1. 入口点发现 - EntryPointDiscovery
 * 2. 调用链路追踪 - CallChainTracer
 * 3. 类型定义导航 - TypeDefinitionNavigator
 * 4. 包结构地图 - PackageStructureMapper
 * 5. 数据流追踪 - DataFlowTracer
 *
 * This is the key integration point that makes the project useful for
 * quickly understanding any Java codebase.
 */
public class CoreAnalysisEngine {

    private final EntryPointDiscovery entryPointDiscovery = new EntryPointDiscovery();
    private final CallChainTracer callChainTracer = new CallChainTracer();
    private final PackageStructureMapper packageMapper = new PackageStructureMapper();
    private final TypeDefinitionNavigator typeNavigator = new TypeDefinitionNavigator();
    private final DataFlowTracer dataFlowTracer = new DataFlowTracer();

    /**
     * Run all core analyses on a project.
     */
    public Map<String, Object> analyze(Path projectRoot) throws IOException {
        System.out.println("\n=== 核心分析引擎启动 ===");

        // 1. Package Structure
        System.out.println("\n📦 正在分析包结构...");
        PackageStructureMapper.PackageNode packageTree = packageMapper.build(projectRoot);

        // 2. Entry Points
        System.out.println("🔍 正在发现入口点...");
        List<EntryPointDiscovery.EntryPoint> entryPoints = entryPointDiscovery.discover(projectRoot);

        // 3. Type Definition Index
        System.out.println("📚 正在构建类型定义索引...");
        typeNavigator.buildIndex(projectRoot);

        // 4. Call Graph & Chain Tracing
        System.out.println("🔗 正在构建调用图...");
        CallChainTracer.CallGraph callGraph = callChainTracer.buildCallGraph(projectRoot);

        // Trace chains from entry points
        List<String> entryPointKeys = new ArrayList<>();
        for (EntryPointDiscovery.EntryPoint ep : entryPoints) {
            entryPointKeys.add(ep.getClassName() + "#" + ep.getMethodName());
        }
        Map<String, List<CallChainTracer.CallChain>> callChains =
                callChainTracer.traceAll(callGraph, entryPointKeys, 5);

        // 5. Data Flow (trace from first few entry points for performance)
        System.out.println("🌊 正在追踪数据流...");
        List<DataFlowTracer.DataFlow> dataFlows = new ArrayList<>();
        int flowCount = 0;
        for (EntryPointDiscovery.EntryPoint ep : entryPoints) {
            if (flowCount >= 5) break; // Limit to first 5 entry points
            String filePath = projectRoot.resolve(ep.getFilePath()).toString();
            try {
                List<DataFlowTracer.DataFlow> epFlows = dataFlowTracer.traceMethod(
                        ep.getClassName(), ep.getMethodName(), filePath);
                dataFlows.addAll(epFlows);
                flowCount++;
            } catch (Exception e) {
                // ignore
            }
        }

        // Build result
        Map<String, Object> result = new LinkedHashMap<>();

        // Package structure
        Map<String, Object> pkgInfo = new LinkedHashMap<>();
        pkgInfo.put("layer_summary", summarizeLayers(packageTree));
        pkgInfo.put("tree", packageTree.toMap());
        result.put("package_structure", pkgInfo);

        // Entry points
        Map<String, Object> entryInfo = new LinkedHashMap<>();
        entryInfo.put("total", entryPoints.size());
        entryInfo.put("by_type", entryPointDiscovery.groupByType(entryPoints).entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        e -> e.getKey().name(),
                        e -> e.getValue().stream().map(EntryPointDiscovery.EntryPoint::toMap)
                                .collect(java.util.stream.Collectors.toList()))));
        entryInfo.put("entry_points", entryPoints.stream()
                .map(EntryPointDiscovery.EntryPoint::toMap)
                .collect(java.util.stream.Collectors.toList()));
        result.put("entry_points", entryInfo);

        // Call graph
        Map<String, Object> callInfo = new LinkedHashMap<>();
        callInfo.put("node_count", callGraph.getNodeCount());
        callInfo.put("edge_count", callGraph.getEdgeCount());
        callInfo.put("chains_from_entry", callChains.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream().limit(5)
                                .map(CallChainTracer.CallChain::toMap)
                                .collect(java.util.stream.Collectors.toList()))));
        result.put("call_graph", callInfo);

        // Type definitions
        result.put("type_definitions", typeNavigator.export());

        // Data flows
        result.put("data_flows", dataFlowTracer.export(dataFlows));

        // Print summary
        printSummary(entryPoints, packageTree, callChains, dataFlows);

        return result;
    }

    private Map<String, Integer> summarizeLayers(PackageStructureMapper.PackageNode root) {
        Map<String, Integer> layers = new LinkedHashMap<>();
        countLayers(root, layers);
        return layers;
    }

    private void countLayers(PackageStructureMapper.PackageNode node, Map<String, Integer> layers) {
        String layer = node.getLayer();
        if (!layer.isEmpty()) {
            layers.merge(layer, node.getTotalClassCount(), Integer::sum);
        }
        for (PackageStructureMapper.PackageNode sub : node.getSubPackages().values()) {
            countLayers(sub, layers);
        }
    }

    /**
     * Print a readable summary of the core analysis.
     */
    public void printSummary(List<EntryPointDiscovery.EntryPoint> entryPoints,
                              PackageStructureMapper.PackageNode packageTree,
                              Map<String, List<CallChainTracer.CallChain>> callChains,
                              List<DataFlowTracer.DataFlow> dataFlows) {
        // Entry points
        System.out.println("\n=== 项目解构报告 ===");
        System.out.println("\n📍 入口点: " + entryPoints.size() + " 个");
        entryPointDiscovery.groupByType(entryPoints).forEach((type, eps) -> {
            System.out.println("  " + type.name() + ": " + eps.size() + " 个");
        });

        // Package structure
        System.out.println("\n📦 包结构:");
        packageMapper.printTree(packageTree);

        // Call chains
        if (!callChains.isEmpty()) {
            callChainTracer.printChains(callChains);
        }

        // Type definitions
        System.out.println("\n📚 类型定义: 已索引 " + typeNavigator.export().get("total_types") + " 个类型");

        // Data flows
        if (!dataFlows.isEmpty()) {
            dataFlowTracer.printFlows(dataFlows);
        }
    }
}
