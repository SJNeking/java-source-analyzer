package cn.dolphinmind.glossary.java.analyze.dataflow;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;

import java.util.*;

/**
 * Data Flow Graph for a Java method.
 * Tracks variable definitions (defs) and uses (uses) to build def-use chains.
 *
 * Each node is a statement. Edges represent data dependencies:
 *   def(x) at statement S1 → use(x) at statement S2
 *
 * This is the foundation for Taint Analysis and reaching definitions.
 */
public class DataFlowGraph {

    private final MethodDeclaration method;
    private final List<DFGNode> nodes = new ArrayList<>();
    private final List<DFGEdge> edges = new ArrayList<>();
    private final Map<String, List<DFGNode>> defs = new LinkedHashMap<>();
    private final Map<String, List<DFGNode>> uses = new LinkedHashMap<>();
    private final Map<Statement, DFGNode> stmtToNode = new IdentityHashMap<>();

    public DataFlowGraph(MethodDeclaration method) {
        this.method = method;
        build();
    }

    public MethodDeclaration getMethod() { return method; }
    public List<DFGNode> getNodes() { return Collections.unmodifiableList(nodes); }
    public List<DFGEdge> getEdges() { return Collections.unmodifiableList(edges); }

    /**
     * Build the DFG by walking all statements in the method body.
     */
    private void build() {
        if (!method.getBody().isPresent()) return;

        BlockStmt body = method.getBody().get();
        int nodeId = 0;

        // First pass: collect all expression statements and their variable defs/uses
        Map<Statement, List<String>> statementDefs = new IdentityHashMap<>();
        Map<Statement, List<String>> statementUses = new IdentityHashMap<>();

        for (Statement stmt : body.findAll(Statement.class)) {
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
            } else if (stmt instanceof DoStmt) {
                collectUses(((DoStmt) stmt).getCondition(), stmtUses);
            } else if (stmt instanceof ForStmt) {
                for (Expression init : ((ForStmt) stmt).getInitialization()) {
                    collectDefs(init, stmtDefs);
                    collectUses(init, stmtUses);
                }
                ((ForStmt) stmt).getCompare().ifPresent(c -> collectUses(c, stmtUses));
                for (Expression update : ((ForStmt) stmt).getUpdate()) {
                    collectDefs(update, stmtDefs);
                    collectUses(update, stmtUses);
                }
            } else if (stmt instanceof com.github.javaparser.ast.stmt.ForEachStmt) {
                com.github.javaparser.ast.stmt.ForEachStmt foreach = (com.github.javaparser.ast.stmt.ForEachStmt) stmt;
                collectDefs(foreach.getVariable(), stmtDefs);
                collectUses(foreach.getIterable(), stmtUses);
            }

            DFGNode node = new DFGNode(nodeId++, stmt, stmtDefs, stmtUses);
            nodes.add(node);
            stmtToNode.put(stmt, node);

            for (String v : stmtDefs) {
                defs.computeIfAbsent(v, k -> new ArrayList<>()).add(node);
            }
            for (String v : stmtUses) {
                uses.computeIfAbsent(v, k -> new ArrayList<>()).add(node);
            }
        }

        // Second pass: build def-use edges (reaching definitions)
        // For each use of variable v at node U, find the most recent def of v that reaches U
        for (Map.Entry<String, List<DFGNode>> useEntry : uses.entrySet()) {
            String var = useEntry.getKey();
            for (DFGNode useNode : useEntry.getValue()) {
                // Find the latest def of var that comes before this use
                DFGNode latestDef = null;
                for (int i = 0; i < useNode.getId(); i++) {
                    DFGNode candidate = nodes.get(i);
                    if (candidate.getDefs().contains(var)) {
                        // Check if there's no intervening re-definition
                        boolean isKilled = false;
                        for (int j = candidate.getId() + 1; j < useNode.getId(); j++) {
                            if (nodes.get(j).getDefs().contains(var)) {
                                isKilled = true;
                                break;
                            }
                        }
                        if (!isKilled) {
                            latestDef = candidate;
                        }
                    }
                }
                if (latestDef != null) {
                    edges.add(new DFGEdge(latestDef, useNode, var));
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
            // obj.field - we track the base object
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
               Character.isUpperCase(name.charAt(0)); // Class names
    }

    /**
     * Get all nodes that define a variable.
     */
    public List<DFGNode> getDefs(String var) {
        return defs.getOrDefault(var, Collections.emptyList());
    }

    /**
     * Get all nodes that use a variable.
     */
    public List<DFGNode> getUses(String var) {
        return uses.getOrDefault(var, Collections.emptyList());
    }

    /**
     * Get all edges for a variable.
     */
    public List<DFGEdge> getEdgesForVar(String var) {
        List<DFGEdge> result = new ArrayList<>();
        for (DFGEdge e : edges) {
            if (e.getVariable().equals(var)) result.add(e);
        }
        return result;
    }

    /**
     * Get the DFG node for a statement.
     */
    public DFGNode getNodeForStatement(Statement stmt) {
        return stmtToNode.get(stmt);
    }

    /**
     * Export DFG as DOT format.
     */
    public String toDot() {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph DFG_").append(method.getNameAsString()).append(" {\n");
        for (DFGNode node : nodes) {
            sb.append("  N").append(node.getId())
              .append(" [shape=record, label=\"N").append(node.getId())
              .append(": ").append(node.getLabel()).append("\\ndef: ").append(node.getDefs())
              .append("\\nuse: ").append(node.getUses()).append("\"];\n");
        }
        for (DFGEdge edge : edges) {
            sb.append("  N").append(edge.getSource().getId())
              .append(" -> N").append(edge.getTarget().getId())
              .append(" [label=\"").append(edge.getVariable()).append("\"];\n");
        }
        sb.append("}\n");
        return sb.toString();
    }
}
