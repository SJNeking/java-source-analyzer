package cn.dolphinmind.glossary.java.analyze.cfg;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.Statement;

import java.util.*;

/**
 * A node in the Control Flow Graph.
 *
 * Types:
 * - ENTRY: synthetic start node
 * - EXIT: synthetic end node
 * - BASIC_BLOCK: sequential statements with one entry and one exit
 * - CONDITIONAL: branching node (if, for, while, do, ternary)
 * - SWITCH: multi-way branching
 * - TRY: try block with potential exception exits
 */
public class CFGNode {

    public enum NodeType {
        ENTRY, EXIT, BASIC_BLOCK, CONDITIONAL, SWITCH, TRY, CATCH, FINALLY
    }

    private final int id;
    private final NodeType type;
    private final Node astNode;
    private final List<Statement> statements;
    private final Set<CFGNode> successors = new LinkedHashSet<>();
    private final Set<CFGNode> predecessors = new LinkedHashSet<>();

    /** Human-readable label for debugging */
    private String label = "";

    public CFGNode(int id, NodeType type, Node astNode) {
        this.id = id;
        this.type = type;
        this.astNode = astNode;
        this.statements = (astNode instanceof Statement) ? Collections.singletonList((Statement) astNode) : Collections.emptyList();
    }

    public CFGNode(int id, NodeType type, Node astNode, String label) {
        this(id, type, astNode);
        this.label = label;
    }

    public int getId() { return id; }
    public NodeType getType() { return type; }
    public Node getAstNode() { return astNode; }
    public List<Statement> getStatements() { return Collections.unmodifiableList(statements); }
    public Set<CFGNode> getSuccessors() { return Collections.unmodifiableSet(successors); }
    public Set<CFGNode> getPredecessors() { return Collections.unmodifiableSet(predecessors); }
    public String getLabel() { return label; }

    public void addSuccessor(CFGNode target) {
        successors.add(target);
        target.predecessors.add(this);
    }

    public void removeSuccessor(CFGNode target) {
        if (successors.remove(target)) {
            target.predecessors.remove(this);
        }
    }

    /** Find all paths from this node to any EXIT node (bounded depth) */
    public List<List<CFGNode>> findAllPaths(int maxDepth) {
        List<List<CFGNode>> result = new ArrayList<>();
        findAllPathsDFS(new ArrayList<>(), new HashSet<>(), result, maxDepth);
        return result;
    }

    private void findAllPathsDFS(List<CFGNode> currentPath, Set<Integer> visited, List<List<CFGNode>> result, int maxDepth) {
        if (currentPath.size() > maxDepth) return;
        if (visited.contains(id)) return; // avoid cycles

        visited.add(id);
        currentPath.add(this);

        if (type == NodeType.EXIT) {
            result.add(new ArrayList<>(currentPath));
        } else if (successors.isEmpty()) {
            // Dead end - still record the path
            result.add(new ArrayList<>(currentPath));
        } else {
            for (CFGNode succ : successors) {
                succ.findAllPathsDFS(currentPath, visited, result, maxDepth);
            }
        }

        currentPath.remove(currentPath.size() - 1);
        visited.remove(id);
    }

    /** Check if this node is reachable from ENTRY */
    public boolean isReachableFrom(CFGNode entry) {
        Set<Integer> visited = new HashSet<>();
        return isReachableDFS(entry, visited);
    }

    private boolean isReachableDFS(CFGNode current, Set<Integer> visited) {
        if (visited.contains(current.id)) return false;
        if (current.id == this.id) return true;
        visited.add(current.id);
        for (CFGNode succ : current.successors) {
            if (isReachableDFS(succ, visited)) return true;
        }
        return false;
    }

    @Override
    public String toString() {
        String typeName = type.name();
        String labelStr = label.isEmpty() ? "" : " [" + label + "]";
        String astInfo = astNode != null ? " line:" + astNode.getBegin().map(p -> p.line).orElse(0) : "";
        return typeName + labelStr + astInfo + " (id=" + id + ")";
    }

    public String toDot() {
        String shape = "ellipse";
        switch (type) {
            case ENTRY: shape = "ellipse"; break;
            case EXIT: shape = "doublecircle"; break;
            case CONDITIONAL: shape = "diamond"; break;
            case SWITCH: shape = "diamond"; break;
            case BASIC_BLOCK: shape = "record"; break;
        }
        String label = this.label.isEmpty() ? type.name() : this.label;
        return "N" + id + " [shape=" + shape + ", label=\"N" + id + ": " + label + "\"]";
    }
}
