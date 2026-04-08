package cn.dolphinmind.glossary.java.analyze.cfg;

import java.util.*;

/**
 * Complete Control Flow Graph for a single Java method.
 *
 * Provides:
 * - True McCabe cyclomatic complexity (E - N + 2P)
 * - Reachability analysis (unreachable code detection)
 * - Path enumeration (bounded depth)
 * - Graph visualization in DOT format
 */
public class ControlFlowGraph {

    private final CFGNode entry;
    private final CFGNode exit;
    private final List<CFGNode> nodes;
    private final List<CFGEdge> edges;
    private final String methodName;

    public ControlFlowGraph(CFGNode entry, CFGNode exit, List<CFGNode> nodes,
                            List<CFGEdge> edges, String methodName) {
        this.entry = entry;
        this.exit = exit;
        this.nodes = Collections.unmodifiableList(nodes);
        this.edges = Collections.unmodifiableList(edges);
        this.methodName = methodName;
    }

    public CFGNode getEntry() { return entry; }
    public CFGNode getExit() { return exit; }
    public List<CFGNode> getNodes() { return nodes; }
    public List<CFGEdge> getEdges() { return edges; }
    public String getMethodName() { return methodName; }

    /**
     * Compute true McCabe cyclomatic complexity: M = E - N + 2P
     * where P = number of connected components (usually 1 for a method).
     */
    public int computeCyclomaticComplexity() {
        int E = edges.size();
        int N = nodes.size();
        int P = countConnectedComponents();
        return E - N + 2 * P;
    }

    /**
     * Count connected components using Union-Find on the undirected version.
     */
    private int countConnectedComponents() {
        Map<Integer, Integer> parent = new HashMap<>();
        for (CFGNode node : nodes) {
            parent.put(node.getId(), node.getId());
        }

        for (CFGEdge edge : edges) {
            int rootA = find(parent, edge.getSource().getId());
            int rootB = find(parent, edge.getTarget().getId());
            if (rootA != rootB) {
                parent.put(rootA, rootB);
            }
        }

        long components = parent.values().stream().distinct().count();
        return (int) components;
    }

    private int find(Map<Integer, Integer> parent, int x) {
        if (parent.get(x) != x) {
            parent.put(x, find(parent, parent.get(x))); // path compression
        }
        return parent.get(x);
    }

    /**
     * Find all unreachable nodes (not reachable from ENTRY).
     */
    public List<CFGNode> findUnreachableNodes() {
        List<CFGNode> unreachable = new ArrayList<>();
        Set<Integer> reachable = new HashSet<>();
        bfsReachable(entry, reachable);
        for (CFGNode node : nodes) {
            if (!reachable.contains(node.getId())) {
                unreachable.add(node);
            }
        }
        return unreachable;
    }

    private void bfsReachable(CFGNode start, Set<Integer> reachable) {
        Queue<CFGNode> queue = new ArrayDeque<>();
        queue.add(start);
        reachable.add(start.getId());
        while (!queue.isEmpty()) {
            CFGNode current = queue.poll();
            for (CFGNode succ : current.getSuccessors()) {
                if (reachable.add(succ.getId())) {
                    queue.add(succ);
                }
            }
        }
    }

    /**
     * Find all paths from ENTRY to EXIT (bounded by maxDepth).
     */
    public List<List<CFGNode>> findAllPaths(int maxDepth) {
        return entry.findAllPaths(maxDepth);
    }

    /**
     * Check if a specific node is reachable from ENTRY.
     */
    public boolean isNodeReachable(int nodeId) {
        CFGNode target = nodes.stream().filter(n -> n.getId() == nodeId).findFirst().orElse(null);
        return target != null && target.isReachableFrom(entry);
    }

    /**
     * Export the CFG as a DOT graph for Graphviz visualization.
     */
    public String toDot() {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph CFG_").append(methodName).append(" {\n");
        sb.append("  rankdir=TB;\n");
        sb.append("  node [fontname=\"Courier\"];\n");
        sb.append("  edge [fontname=\"Courier\", fontsize=10];\n");
        sb.append("\n");

        for (CFGNode node : nodes) {
            sb.append("  ").append(node.toDot()).append(";\n");
        }
        sb.append("\n");

        for (CFGEdge edge : edges) {
            String color = "black";
            switch (edge.getType()) {
                case TRUE_BRANCH: color = "green"; break;
                case FALSE_BRANCH: color = "red"; break;
                case EXCEPTION: color = "orange"; break;
                case RETURN: color = "blue"; break;
                case THROW: color = "purple"; break;
                case CONTINUE: color = "brown"; break;
                case BREAK: color = "gray"; break;
            }
            sb.append("  N").append(edge.getSource().getId())
              .append(" -> N").append(edge.getTarget().getId())
              .append(" [color=").append(color)
              .append(", label=\"").append(edge.getType()).append("\"];\n");
        }

        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Get summary statistics.
     */
    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("method", methodName);
        summary.put("node_count", nodes.size());
        summary.put("edge_count", edges.size());
        summary.put("cyclomatic_complexity", computeCyclomaticComplexity());
        summary.put("unreachable_nodes", findUnreachableNodes().size());
        return summary;
    }
}
