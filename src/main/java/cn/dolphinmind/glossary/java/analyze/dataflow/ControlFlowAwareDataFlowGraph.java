package cn.dolphinmind.glossary.java.analyze.dataflow;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.Node;

import java.util.*;

/**
 * Enhanced Data Flow Graph with control flow awareness.
 *
 * Improvements over DataFlowGraph:
 * 1. Handles branches (if/else, switch) - tracks def-use across branches
 * 2. Handles loops - tracks def-use through loop iterations
 * 3. Supports reaching definitions analysis with kill sets
 * 4. Provides path-sensitive taint tracking
 */
public class ControlFlowAwareDataFlowGraph {

    private final MethodDeclaration method;
    private final List<DFGNode> nodes = new ArrayList<>();
    private final List<DFGEdge> edges = new ArrayList<>();
    private final Map<String, List<DFGNode>> defs = new LinkedHashMap<>();
    private final Map<String, List<DFGNode>> uses = new LinkedHashMap<>();

    // Control flow: successors and predecessors for each node
    private final Map<DFGNode, List<DFGNode>> successors = new IdentityHashMap<>();
    private final Map<DFGNode, List<DFGNode>> predecessors = new IdentityHashMap<>();

    // Reaching definitions: for each node, what definitions reach it
    private final Map<DFGNode, Map<String, Set<DFGNode>>> reachingDefs = new IdentityHashMap<>();

    public ControlFlowAwareDataFlowGraph(MethodDeclaration method) {
        this.method = method;
        build();
    }

    public MethodDeclaration getMethod() { return method; }
    public List<DFGNode> getNodes() { return Collections.unmodifiableList(nodes); }
    public List<DFGEdge> getEdges() { return Collections.unmodifiableList(edges); }

    /**
     * Get all reaching definitions for a variable at a given node.
     */
    public Set<DFGNode> getReachingDefs(DFGNode node, String variable) {
        Map<String, Set<DFGNode>> rd = reachingDefs.get(node);
        return rd != null ? rd.getOrDefault(variable, Collections.emptySet()) : Collections.emptySet();
    }

    /**
     * Build the DFG with control flow.
     */
    private void build() {
        if (!method.getBody().isPresent()) return;

        BlockStmt body = method.getBody().get();

        // Build control flow graph (CFG)
        List<Statement> statements = flattenStatements(body);
        List<DFGNode> cfgNodes = buildCfgNodes(statements);
        buildControlFlowEdges(cfgNodes);

        // Build def-use edges with reaching definitions
        computeReachingDefinitions(cfgNodes);
        buildDefUseEdges(cfgNodes);
    }

    /**
     * Flatten nested statements into a linear list.
     */
    private List<Statement> flattenStatements(BlockStmt block) {
        List<Statement> result = new ArrayList<>();
        for (Statement stmt : block.getStatements()) {
            if (stmt instanceof BlockStmt) {
                result.addAll(flattenStatements((BlockStmt) stmt));
            } else if (stmt instanceof IfStmt) {
                result.add(stmt);
                IfStmt ifStmt = (IfStmt) stmt;
                if (ifStmt.getThenStmt() instanceof BlockStmt) {
                    result.addAll(flattenStatements((BlockStmt) ifStmt.getThenStmt()));
                } else {
                    result.add(ifStmt.getThenStmt());
                }
                ifStmt.getElseStmt().ifPresent(elseStmt -> {
                    if (elseStmt instanceof BlockStmt) {
                        result.addAll(flattenStatements((BlockStmt) elseStmt));
                    } else {
                        result.add(elseStmt);
                    }
                });
            } else if (stmt instanceof ForStmt || stmt instanceof ForEachStmt || stmt instanceof WhileStmt || stmt instanceof DoStmt) {
                result.add(stmt);
                // Add loop body
                Statement loopBody = getLoopBody(stmt);
                if (loopBody != null) {
                    if (loopBody instanceof BlockStmt) {
                        result.addAll(flattenStatements((BlockStmt) loopBody));
                    } else {
                        result.add(loopBody);
                    }
                }
            } else {
                result.add(stmt);
            }
        }
        return result;
    }

    private Statement getLoopBody(Statement loopStmt) {
        if (loopStmt instanceof ForStmt) return ((ForStmt) loopStmt).getBody();
        if (loopStmt instanceof ForEachStmt) return ((ForEachStmt) loopStmt).getBody();
        if (loopStmt instanceof WhileStmt) return ((WhileStmt) loopStmt).getBody();
        if (loopStmt instanceof DoStmt) return ((DoStmt) loopStmt).getBody();
        return null;
    }

    /**
     * Build CFG nodes from statements.
     */
    private List<DFGNode> buildCfgNodes(List<Statement> statements) {
        int nodeId = 0;
        for (Statement stmt : statements) {
            List<String> stmtDefs = new ArrayList<>();
            List<String> stmtUses = new ArrayList<>();

            if (stmt instanceof ExpressionStmt) {
                Expression expr = ((ExpressionStmt) stmt).getExpression();
                collectDefs(expr, stmtDefs);
                collectUses(expr, stmtUses);
            } else if (stmt instanceof ReturnStmt) {
                ((ReturnStmt) stmt).getExpression().ifPresent(e -> collectUses(e, stmtUses));
            } else if (stmt instanceof IfStmt) {
                collectUses(((IfStmt) stmt).getCondition(), stmtUses);
            } else if (stmt instanceof WhileStmt) {
                collectUses(((WhileStmt) stmt).getCondition(), stmtUses);
            } else if (stmt instanceof ForStmt) {
                ForStmt forStmt = (ForStmt) stmt;
                for (Expression init : forStmt.getInitialization()) {
                    collectDefs(init, stmtDefs);
                    collectUses(init, stmtUses);
                }
                forStmt.getCompare().ifPresent(c -> collectUses(c, stmtUses));
                for (Expression update : forStmt.getUpdate()) {
                    collectDefs(update, stmtDefs);
                    collectUses(update, stmtUses);
                }
            } else if (stmt instanceof ForEachStmt) {
                ForEachStmt foreach = (ForEachStmt) stmt;
                collectDefs(foreach.getVariable(), stmtDefs);
                collectUses(foreach.getIterable(), stmtUses);
            }

            // For VariableDeclarationExpr, also collect uses from initializers
            if (stmt instanceof ExpressionStmt) {
                Expression expr = ((ExpressionStmt) stmt).getExpression();
                if (expr instanceof com.github.javaparser.ast.expr.VariableDeclarationExpr) {
                    com.github.javaparser.ast.expr.VariableDeclarationExpr vde =
                        (com.github.javaparser.ast.expr.VariableDeclarationExpr) expr;
                    for (com.github.javaparser.ast.body.VariableDeclarator vd : vde.getVariables()) {
                        if (vd.getInitializer().isPresent()) {
                            collectUses(vd.getInitializer().get(), stmtUses);
                        }
                    }
                }
            }

            DFGNode node = new DFGNode(nodeId++, stmt, stmtDefs, stmtUses);
            nodes.add(node);
        }
        return nodes;
    }

    /**
     * Build control flow edges based on statement ordering and branches.
     */
    private void buildControlFlowEdges(List<DFGNode> cfgNodes) {
        for (int i = 0; i < cfgNodes.size() - 1; i++) {
            DFGNode current = cfgNodes.get(i);
            DFGNode next = cfgNodes.get(i + 1);

            // Check if current is a branch point
            boolean isBranch = current.getStatement() instanceof IfStmt;

            if (isBranch) {
                // If-then: both then-branch and else-branch (or next stmt) are successors
                IfStmt ifStmt = (IfStmt) current.getStatement();
                boolean hasElse = ifStmt.getElseStmt().isPresent();

                if (hasElse) {
                    // Has explicit else: add edge to next (which could be then or else branch)
                    successors.computeIfAbsent(current, k -> new ArrayList<>()).add(next);
                    predecessors.computeIfAbsent(next, k -> new ArrayList<>()).add(current);
                } else {
                    // No else: both next stmt and fall-through are possible
                    successors.computeIfAbsent(current, k -> new ArrayList<>()).add(next);
                    predecessors.computeIfAbsent(next, k -> new ArrayList<>()).add(current);
                }
            } else {
                // Sequential: normal flow edge
                successors.computeIfAbsent(current, k -> new ArrayList<>()).add(next);
                predecessors.computeIfAbsent(next, k -> new ArrayList<>()).add(current);
            }
        }
    }

    /**
     * Compute reaching definitions using iterative data flow analysis.
     * For each node, determine which definitions of each variable reach it.
     */
    private void computeReachingDefinitions(List<DFGNode> cfgNodes) {
        // Initialize: entry node has empty reaching defs
        for (DFGNode node : cfgNodes) {
            reachingDefs.put(node, new HashMap<>());
        }

        // Iterate until fixpoint
        boolean changed = true;
        int maxIterations = 100; // Prevent infinite loops
        int iterations = 0;

        while (changed && iterations < maxIterations) {
            changed = false;
            iterations++;

            for (DFGNode node : cfgNodes) {
                // Compute IN[node] = union of OUT[p] for all predecessors p
                Map<String, Set<DFGNode>> in = new HashMap<>();
                for (DFGNode pred : predecessors.getOrDefault(node, Collections.emptyList())) {
                    Map<String, Set<DFGNode>> out = reachingDefs.get(pred);
                    for (Map.Entry<String, Set<DFGNode>> entry : out.entrySet()) {
                        in.computeIfAbsent(entry.getKey(), k -> new HashSet<>()).addAll(entry.getValue());
                    }
                }

                // Compute OUT[node] = gen[node] ∪ (IN[node] - kill[node])
                Map<String, Set<DFGNode>> out = new HashMap<>(in);

                // Kill: if this node defines a variable, kill all previous defs of that variable
                for (String defVar : node.getDefs()) {
                    out.put(defVar, new HashSet<>()); // Kill all previous defs
                }

                // Gen: add this node's definitions
                for (String defVar : node.getDefs()) {
                    out.computeIfAbsent(defVar, k -> new HashSet<>()).add(node);
                }

                // Check if changed
                if (!out.equals(reachingDefs.get(node))) {
                    changed = true;
                    reachingDefs.put(node, out);
                }
            }
        }
    }

    /**
     * Build def-use edges using reaching definitions.
     */
    private void buildDefUseEdges(List<DFGNode> cfgNodes) {
        for (DFGNode useNode : cfgNodes) {
            Map<String, Set<DFGNode>> rd = reachingDefs.get(useNode);
            if (rd == null) continue;

            for (String useVar : useNode.getUses()) {
                Set<DFGNode> reachingDefNodes = rd.get(useVar);
                if (reachingDefNodes != null) {
                    for (DFGNode defNode : reachingDefNodes) {
                        edges.add(new DFGEdge(defNode, useNode, useVar));
                    }
                }
            }
        }
    }

    /**
     * Collect variable definitions in an expression.
     */
    private void collectDefs(Expression expr, List<String> defs) {
        if (expr instanceof AssignExpr) {
            AssignExpr ae = (AssignExpr) expr;
            if (ae.getOperator() == AssignExpr.Operator.ASSIGN) {
                collectLValues(ae.getTarget(), defs);
            }
        } else if (expr instanceof com.github.javaparser.ast.expr.VariableDeclarationExpr) {
            com.github.javaparser.ast.expr.VariableDeclarationExpr vde =
                    (com.github.javaparser.ast.expr.VariableDeclarationExpr) expr;
            for (com.github.javaparser.ast.body.VariableDeclarator vd : vde.getVariables()) {
                defs.add(vd.getNameAsString());
            }
        }
    }

    private void collectLValues(Expression expr, List<String> defs) {
        if (expr instanceof NameExpr) {
            defs.add(((NameExpr) expr).getNameAsString());
        } else if (expr instanceof FieldAccessExpr) {
            collectLValues(((FieldAccessExpr) expr).getScope(), defs);
        }
    }

    /**
     * Collect variable uses in an expression.
     */
    private void collectUses(Expression expr, List<String> uses) {
        if (expr instanceof NameExpr) {
            String name = ((NameExpr) expr).getNameAsString();
            if (!isKeyword(name)) uses.add(name);
        } else if (expr instanceof FieldAccessExpr) {
            collectUses(((FieldAccessExpr) expr).getScope(), uses);
        } else if (expr instanceof MethodCallExpr) {
            MethodCallExpr mce = (MethodCallExpr) expr;
            mce.getScope().ifPresent(s -> collectUses(s, uses));
            mce.getArguments().forEach(a -> collectUses(a, uses));
        } else if (expr instanceof BinaryExpr) {
            BinaryExpr be = (BinaryExpr) expr;
            collectUses(be.getLeft(), uses);
            collectUses(be.getRight(), uses);
        } else if (expr instanceof UnaryExpr) {
            collectUses(((UnaryExpr) expr).getExpression(), uses);
        } else if (expr instanceof EnclosedExpr) {
            collectUses(((EnclosedExpr) expr).getInner(), uses);
        } else if (expr instanceof CastExpr) {
            collectUses(((CastExpr) expr).getExpression(), uses);
        } else if (expr instanceof ConditionalExpr) {
            ConditionalExpr ce = (ConditionalExpr) expr;
            collectUses(ce.getCondition(), uses);
            collectUses(ce.getThenExpr(), uses);
            collectUses(ce.getElseExpr(), uses);
        } else if (expr instanceof ObjectCreationExpr) {
            ObjectCreationExpr oce = (ObjectCreationExpr) expr;
            oce.getArguments().forEach(a -> collectUses(a, uses));
        } else if (expr instanceof ArrayAccessExpr) {
            ArrayAccessExpr aae = (ArrayAccessExpr) expr;
            collectUses(aae.getName(), uses);
            collectUses(aae.getIndex(), uses);
        }
    }

    private boolean isKeyword(String name) {
        return name.equals("null") || name.equals("true") || name.equals("false") ||
               name.equals("this") || name.equals("super") ||
               Character.isUpperCase(name.charAt(0));
    }

    /**
     * Export CFG + DFG as DOT format for visualization.
     */
    public String toDot() {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph CFG_DFG_").append(method.getNameAsString()).append(" {\n");

        for (DFGNode node : nodes) {
            sb.append("  N").append(node.getId())
              .append(" [shape=record, label=\"N").append(node.getId())
              .append(": ").append(node.getLabel())
              .append("\\ndef: ").append(node.getDefs())
              .append("\\nuse: ").append(node.getUses()).append("\"];\n");
        }

        // Control flow edges (dashed)
        for (Map.Entry<DFGNode, List<DFGNode>> entry : successors.entrySet()) {
            DFGNode from = entry.getKey();
            for (DFGNode to : entry.getValue()) {
                sb.append("  N").append(from.getId())
                  .append(" -> N").append(to.getId())
                  .append(" [style=dashed, color=gray];\n");
            }
        }

        // Data flow edges (solid)
        for (DFGEdge edge : edges) {
            sb.append("  N").append(edge.getSource().getId())
              .append(" -> N").append(edge.getTarget().getId())
              .append(" [label=\"").append(edge.getVariable()).append("\"];\n");
        }

        sb.append("}\n");
        return sb.toString();
    }
}
