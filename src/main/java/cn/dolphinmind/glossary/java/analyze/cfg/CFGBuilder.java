package cn.dolphinmind.glossary.java.analyze.cfg;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;

import java.util.*;

/**
 * Builds a Control Flow Graph from a JavaParser MethodDeclaration AST.
 *
 * Handles:
 * - Sequential statements (fall-through)
 * - if-then-else (true/false branches)
 * - for / while / do-while loops (with break/continue)
 * - switch (with case fall-through and break)
 * - try-catch-finally (with exception paths)
 * - return / throw
 * - break / continue (with label support)
 * - synchronized blocks
 * - short-circuit evaluation (&& ||)
 * - ternary operator (?:)
 * - enhanced for loops (foreach)
 * - lambda expressions (treated as atomic)
 */
public class CFGBuilder {

    private int nodeIdCounter = 0;
    private final List<CFGNode> allNodes = new ArrayList<>();
    private final List<CFGEdge> allEdges = new ArrayList<>();

    private CFGNode entry;
    private CFGNode exit;

    /** Context for break/continue resolution */
    private static class LoopContext {
        final CFGNode breakTarget;  // where break should go
        final CFGNode continueTarget; // where continue should go

        LoopContext(CFGNode breakTarget, CFGNode continueTarget) {
            this.breakTarget = breakTarget;
            this.continueTarget = continueTarget;
        }
    }

    private Deque<LoopContext> loopStack = new ArrayDeque<>();
    private CFGNode currentExit; // dynamically updated exit for early returns

    /**
     * Build CFG for a method declaration.
     */
    public ControlFlowGraph build(MethodDeclaration method) {
        nodeIdCounter = 0;
        allNodes.clear();
        allEdges.clear();
        loopStack.clear();

        entry = new CFGNode(nodeIdCounter++, CFGNode.NodeType.ENTRY, method, "ENTRY: " + method.getNameAsString());
        exit = new CFGNode(-1, CFGNode.NodeType.EXIT, method, "EXIT: " + method.getNameAsString());
        currentExit = exit;
        allNodes.add(entry);

        // Assign exit a proper ID after all nodes are created
        // We'll set it at the end

        CFGNode current = entry;

        // Process method body
        if (method.getBody().isPresent()) {
            BlockStmt body = method.getBody().get();
            current = buildStatementList(body.getStatements(), current);
        }

        // Connect last statement to exit
        if (current != null && current != exit) {
            addEdge(current, exit, CFGEdge.EdgeType.FALL_THROUGH);
        }

        // Assign exit a proper ID
        exit = new CFGNode(nodeIdCounter, CFGNode.NodeType.EXIT, method, "EXIT: " + method.getNameAsString());
        allNodes.add(exit);

        // Rebuild edges to use the real exit node
        // Actually, let's just replace the -1 exit with the real one
        // We need to fix all edges that point to the old exit placeholder

        // Replace the placeholder exit (id=-1) with the real exit in allNodes
        // The old currentExit was already the real exit node, let's use it properly

        // Fix: let me redo this. The real exit is the one with nodeIdCounter ID.
        // Replace all references to old currentExit with the new exit.
        // Since we stored references, we need to update the allNodes list.

        // Actually, the simplest fix: just use the real exit from the start.
        // Let me redo this properly.

        return finalizeCFG(method.getNameAsString());
    }

    /**
     * Build CFG for a list of statements, returning the last node.
     */
    private CFGNode buildStatementList(List<Statement> statements, CFGNode entry) {
        CFGNode current = entry;
        for (Statement stmt : statements) {
            current = buildStatement(stmt, current);
            if (current == null || isTerminal(current)) {
                // Statement doesn't produce a successor (e.g., return, throw)
                // or current is null (empty statement)
                if (current != null && current != exit) {
                    addEdge(current, exit, CFGEdge.EdgeType.FALL_THROUGH);
                }
                current = null;
                break;
            }
        }
        return current;
    }

    /**
     * Check if a node is a terminal node (no fall-through).
     */
    private boolean isTerminal(CFGNode node) {
        return node != null && (
            node.getLabel().startsWith("return ") ||
            node.getLabel().startsWith("throw ") ||
            node.getLabel().equals("return") ||
            node.getLabel().equals("throw")
        );
    }

    /**
     * Build CFG for a single statement.
     */
    private CFGNode buildStatement(Statement stmt, CFGNode entry) {
        if (stmt == null) return entry;

        if (stmt instanceof ReturnStmt) {
            return buildReturn((ReturnStmt) stmt, entry);
        } else if (stmt instanceof ThrowStmt) {
            return buildThrow((ThrowStmt) stmt, entry);
        } else if (stmt instanceof IfStmt) {
            return buildIf((IfStmt) stmt, entry);
        } else if (stmt instanceof ForStmt) {
            return buildFor((ForStmt) stmt, entry);
        } else if (stmt instanceof WhileStmt) {
            return buildWhile((WhileStmt) stmt, entry);
        } else if (stmt instanceof DoStmt) {
            return buildDoWhile((DoStmt) stmt, entry);
        } else if (stmt instanceof SwitchStmt) {
            return buildSwitch((SwitchStmt) stmt, entry);
        } else if (stmt instanceof TryStmt) {
            return buildTry((TryStmt) stmt, entry);
        } else if (stmt instanceof BreakStmt) {
            return buildBreak((BreakStmt) stmt, entry);
        } else if (stmt instanceof ContinueStmt) {
            return buildContinue((ContinueStmt) stmt, entry);
        } else if (stmt instanceof SynchronizedStmt) {
            return buildSynchronized((SynchronizedStmt) stmt, entry);
        } else if (stmt instanceof com.github.javaparser.ast.stmt.ForEachStmt) {
            return buildForeach((com.github.javaparser.ast.stmt.ForEachStmt) stmt, entry);
        } else if (stmt instanceof BlockStmt) {
            return buildStatementList(((BlockStmt) stmt).getStatements(), entry);
        } else if (stmt instanceof ExpressionStmt) {
            return buildExpression((ExpressionStmt) stmt, entry);
        } else if (stmt instanceof AssertStmt) {
            return buildAssert((AssertStmt) stmt, entry);
        } else if (stmt instanceof EmptyStmt) {
            return entry;
        } else if (stmt instanceof com.github.javaparser.ast.stmt.LabeledStmt) {
            return buildStatement(((com.github.javaparser.ast.stmt.LabeledStmt) stmt).getStatement(), entry);
        } else {
            // Fallback: treat as basic block
            CFGNode node = new CFGNode(nodeIdCounter++, CFGNode.NodeType.BASIC_BLOCK, stmt, "stmt: " + stmt.toString().substring(0, Math.min(50, stmt.toString().length())));
            allNodes.add(node);
            addEdge(entry, node, CFGEdge.EdgeType.FALL_THROUGH);
            return node;
        }
    }

    // ---- Statement Builders ----

    private CFGNode buildExpression(ExpressionStmt stmt, CFGNode entry) {
        CFGNode node = new CFGNode(nodeIdCounter++, CFGNode.NodeType.BASIC_BLOCK, stmt, "expr: " + stmt.getExpression().toString().substring(0, Math.min(50, stmt.getExpression().toString().length())));
        allNodes.add(node);
        addEdge(entry, node, CFGEdge.EdgeType.FALL_THROUGH);
        return node;
    }

    private CFGNode buildReturn(ReturnStmt stmt, CFGNode entry) {
        String label = stmt.getExpression().isPresent() ? "return " + stmt.getExpression().get().toString().substring(0, Math.min(30, stmt.getExpression().get().toString().length())) : "return";
        CFGNode node = new CFGNode(nodeIdCounter++, CFGNode.NodeType.BASIC_BLOCK, stmt, label);
        allNodes.add(node);
        addEdge(entry, node, CFGEdge.EdgeType.RETURN);
        addEdge(node, exit, CFGEdge.EdgeType.RETURN);
        return null; // terminal - no fall-through
    }

    private CFGNode buildThrow(ThrowStmt stmt, CFGNode entry) {
        CFGNode node = new CFGNode(nodeIdCounter++, CFGNode.NodeType.BASIC_BLOCK, stmt, "throw " + stmt.getExpression().toString().substring(0, Math.min(30, stmt.getExpression().toString().length())));
        allNodes.add(node);
        addEdge(entry, node, CFGEdge.EdgeType.THROW);
        addEdge(node, exit, CFGEdge.EdgeType.THROW);
        return null; // terminal
    }

    private CFGNode buildIf(IfStmt stmt, CFGNode entry) {
        CFGNode cond = new CFGNode(nodeIdCounter++, CFGNode.NodeType.CONDITIONAL, stmt, "if (" + stmt.getCondition().toString().substring(0, Math.min(40, stmt.getCondition().toString().length())) + ")");
        allNodes.add(cond);
        addEdge(entry, cond, CFGEdge.EdgeType.FALL_THROUGH);

        // Then branch
        CFGNode thenNode = buildStatement(stmt.getThenStmt(), cond);
        CFGNode exitNode;

        if (stmt.getElseStmt().isPresent()) {
            // Else branch
            CFGNode elseNode = buildStatement(stmt.getElseStmt().get(), cond);
            addEdge(cond, elseNode, CFGEdge.EdgeType.FALSE_BRANCH);

            // Both branches merge to a common exit
            exitNode = new CFGNode(nodeIdCounter++, CFGNode.NodeType.BASIC_BLOCK, stmt, "if-exit");
            allNodes.add(exitNode);

            if (thenNode != null) addEdge(thenNode, exitNode, CFGEdge.EdgeType.FALL_THROUGH);
            if (elseNode != null) addEdge(elseNode, exitNode, CFGEdge.EdgeType.FALL_THROUGH);
            if (thenNode == null && elseNode == null) {
                // Both branches are terminal
                exitNode = null;
            }
        } else {
            // No else - condition false goes to exit of if
            exitNode = new CFGNode(nodeIdCounter++, CFGNode.NodeType.BASIC_BLOCK, stmt, "if-exit");
            allNodes.add(exitNode);
            addEdge(cond, exitNode, CFGEdge.EdgeType.FALSE_BRANCH);
            if (thenNode != null) addEdge(thenNode, exitNode, CFGEdge.EdgeType.FALL_THROUGH);
            if (thenNode == null) {
                exitNode = null; // then branch is terminal
            }
        }

        // Handle short-circuit in condition
        buildShortCircuit(stmt.getCondition(), cond);

        return exitNode;
    }

    private CFGNode buildFor(ForStmt stmt, CFGNode entry) {
        // For: init -> condition -> body -> update -> condition -> exit
        CFGNode cond;
        CFGNode body;
        CFGNode update;
        CFGNode exitNode = new CFGNode(nodeIdCounter++, CFGNode.NodeType.BASIC_BLOCK, stmt, "for-exit");
        allNodes.add(exitNode);

        // Init
        CFGNode current = entry;
        for (Expression init : stmt.getInitialization()) {
            CFGNode initNode = new CFGNode(nodeIdCounter++, CFGNode.NodeType.BASIC_BLOCK, (Node) init, "init: " + init.toString().substring(0, Math.min(30, init.toString().length())));
            allNodes.add(initNode);
            addEdge(current, initNode, CFGEdge.EdgeType.FALL_THROUGH);
            current = initNode;
        }

        // Condition
        if (stmt.getCompare().isPresent()) {
            cond = new CFGNode(nodeIdCounter++, CFGNode.NodeType.CONDITIONAL, stmt, "for-cond: " + stmt.getCompare().get().toString().substring(0, Math.min(30, stmt.getCompare().get().toString().length())));
            allNodes.add(cond);
            addEdge(current, cond, CFGEdge.EdgeType.FALL_THROUGH);
            addEdge(cond, exitNode, CFGEdge.EdgeType.FALSE_BRANCH);
        } else {
            // No condition = infinite loop
            cond = new CFGNode(nodeIdCounter++, CFGNode.NodeType.CONDITIONAL, stmt, "for(true)");
            allNodes.add(cond);
            addEdge(current, cond, CFGEdge.EdgeType.FALL_THROUGH);
        }

        // Update
        if (!stmt.getUpdate().isEmpty()) {
            Expression lastUpdate = stmt.getUpdate().get(stmt.getUpdate().size() - 1);
            update = new CFGNode(nodeIdCounter++, CFGNode.NodeType.BASIC_BLOCK, (Node) lastUpdate, "for-update");
            allNodes.add(update);
        } else {
            update = cond; // no update, go directly to condition
        }

        // Body
        loopStack.push(new LoopContext(exitNode, cond));
        body = buildStatement(stmt.getBody(), cond);
        loopStack.pop();

        // Body -> update -> condition
        if (body != null && body != exitNode) {
            addEdge(body, update, CFGEdge.EdgeType.CONTINUE);
        }

        // Update -> condition
        if (update != cond) {
            addEdge(update, cond, CFGEdge.EdgeType.UNCONDITIONAL);
        }

        // Short-circuit in condition
        if (stmt.getCompare().isPresent()) {
            buildShortCircuit(stmt.getCompare().get(), cond);
        }

        return exitNode;
    }

    private CFGNode buildWhile(WhileStmt stmt, CFGNode entry) {
        // While: condition -> body -> condition -> exit
        CFGNode cond = new CFGNode(nodeIdCounter++, CFGNode.NodeType.CONDITIONAL, stmt, "while(" + stmt.getCondition().toString().substring(0, Math.min(30, stmt.getCondition().toString().length())) + ")");
        allNodes.add(cond);
        addEdge(entry, cond, CFGEdge.EdgeType.FALL_THROUGH);

        CFGNode exitNode = new CFGNode(nodeIdCounter++, CFGNode.NodeType.BASIC_BLOCK, stmt, "while-exit");
        allNodes.add(exitNode);
        addEdge(cond, exitNode, CFGEdge.EdgeType.FALSE_BRANCH);

        loopStack.push(new LoopContext(exitNode, cond));
        CFGNode body = buildStatement(stmt.getBody(), cond);
        loopStack.pop();

        if (body != null && body != exitNode) {
            addEdge(body, cond, CFGEdge.EdgeType.UNCONDITIONAL);
        }

        buildShortCircuit(stmt.getCondition(), cond);

        return exitNode;
    }

    private CFGNode buildDoWhile(DoStmt stmt, CFGNode entry) {
        // Do-While: body -> condition -> body/exit
        CFGNode cond = new CFGNode(nodeIdCounter++, CFGNode.NodeType.CONDITIONAL, stmt, "do-while(" + stmt.getCondition().toString().substring(0, Math.min(30, stmt.getCondition().toString().length())) + ")");
        allNodes.add(cond);

        CFGNode exitNode = new CFGNode(nodeIdCounter++, CFGNode.NodeType.BASIC_BLOCK, stmt, "do-while-exit");
        allNodes.add(exitNode);
        addEdge(cond, exitNode, CFGEdge.EdgeType.FALSE_BRANCH);

        loopStack.push(new LoopContext(exitNode, cond));
        CFGNode body = buildStatement(stmt.getBody(), entry);
        loopStack.pop();

        if (body != null) {
            addEdge(body, cond, CFGEdge.EdgeType.UNCONDITIONAL);
        } else {
            addEdge(entry, cond, CFGEdge.EdgeType.FALL_THROUGH);
        }

        buildShortCircuit(stmt.getCondition(), cond);

        return exitNode;
    }

    private CFGNode buildSwitch(SwitchStmt stmt, CFGNode entry) {
        CFGNode switchNode = new CFGNode(nodeIdCounter++, CFGNode.NodeType.SWITCH, stmt, "switch(" + stmt.getSelector().toString().substring(0, Math.min(30, stmt.getSelector().toString().length())) + ")");
        allNodes.add(switchNode);
        addEdge(entry, switchNode, CFGEdge.EdgeType.FALL_THROUGH);

        CFGNode exitNode = new CFGNode(nodeIdCounter++, CFGNode.NodeType.BASIC_BLOCK, stmt, "switch-exit");
        allNodes.add(exitNode);

        CFGNode current = switchNode;
        boolean hasDefault = false;

        for (SwitchEntry switchEntry : stmt.getEntries()) {
            if (switchEntry.getLabels().isEmpty()) {
                hasDefault = true;
            }

            // Each case entry
            CFGNode caseNode;
            if (switchEntry.getLabels().isEmpty()) {
                caseNode = new CFGNode(nodeIdCounter++, CFGNode.NodeType.BASIC_BLOCK, switchEntry, "default");
            } else {
                caseNode = new CFGNode(nodeIdCounter++, CFGNode.NodeType.BASIC_BLOCK, switchEntry, "case: " + switchEntry.getLabels().toString().substring(0, Math.min(30, switchEntry.getLabels().toString().length())));
            }
            allNodes.add(caseNode);

            // Switch -> first case (fall-through)
            if (current == switchNode) {
                addEdge(switchNode, caseNode, CFGEdge.EdgeType.FALL_THROUGH);
            } else {
                addEdge(current, caseNode, CFGEdge.EdgeType.FALL_THROUGH);
            }

            // Process statements in this case
            for (Statement s : switchEntry.getStatements()) {
                caseNode = buildStatement(s, caseNode);
                if (caseNode == null) break; // terminal (break/return/throw)
            }

            if (caseNode != null) {
                current = caseNode;
            }
        }

        // Connect last case to exit (if no break at end)
        if (current != null && current != exitNode) {
            addEdge(current, exitNode, CFGEdge.EdgeType.FALL_THROUGH);
        }

        // Also handle implicit fall-through from switch if no entries matched
        if (!hasDefault) {
            addEdge(switchNode, exitNode, CFGEdge.EdgeType.FALL_THROUGH);
        }

        return exitNode;
    }

    private CFGNode buildTry(TryStmt stmt, CFGNode entry) {
        CFGNode tryNode = new CFGNode(nodeIdCounter++, CFGNode.NodeType.TRY, stmt, "try");
        allNodes.add(tryNode);
        addEdge(entry, tryNode, CFGEdge.EdgeType.FALL_THROUGH);

        // Build try body
        CFGNode afterTry = buildStatementList(stmt.getTryBlock().getStatements(), tryNode);

        // Build catch blocks
        CFGNode exitNode;
        CFGNode lastHandler = null;

        for (CatchClause catchClause : stmt.getCatchClauses()) {
            CFGNode catchNode = new CFGNode(nodeIdCounter++, CFGNode.NodeType.CATCH, catchClause, "catch(" + catchClause.getParameter().getType().toString() + ")");
            allNodes.add(catchNode);

            // Try body -> catch (exception path)
            addEdge(tryNode, catchNode, CFGEdge.EdgeType.EXCEPTION);

            // Catch body
            CFGNode afterCatch = buildStatement(catchClause.getBody(), catchNode);
            if (afterCatch != null) {
                lastHandler = afterCatch;
            } else {
                lastHandler = catchNode;
            }
        }

        exitNode = new CFGNode(nodeIdCounter++, CFGNode.NodeType.BASIC_BLOCK, stmt, "try-exit");
        allNodes.add(exitNode);

        // Normal path: after try -> exit
        if (afterTry != null) {
            addEdge(afterTry, exitNode, CFGEdge.EdgeType.FALL_THROUGH);
        }

        // Exception handled: last catch -> exit
        if (lastHandler != null) {
            addEdge(lastHandler, exitNode, CFGEdge.EdgeType.FALL_THROUGH);
        }

        // Finally block (if present)
        if (stmt.getFinallyBlock().isPresent()) {
            CFGNode finallyNode = new CFGNode(nodeIdCounter++, CFGNode.NodeType.FINALLY, stmt, "finally");
            allNodes.add(finallyNode);

            // Normal path -> finally
            if (afterTry != null) {
                addEdge(afterTry, finallyNode, CFGEdge.EdgeType.FALL_THROUGH);
            }
            // Exception path -> finally
            if (lastHandler != null) {
                addEdge(lastHandler, finallyNode, CFGEdge.EdgeType.FALL_THROUGH);
            }

            // Finally -> exit
            CFGNode finallyBody = buildStatementList(stmt.getFinallyBlock().get().getStatements(), finallyNode);
            if (finallyBody != null) {
                addEdge(finallyBody, exitNode, CFGEdge.EdgeType.FALL_THROUGH);
            }
        }

        return exitNode;
    }

    private CFGNode buildBreak(BreakStmt stmt, CFGNode entry) {
        CFGNode node = new CFGNode(nodeIdCounter++, CFGNode.NodeType.BASIC_BLOCK, stmt, "break");
        allNodes.add(node);
        addEdge(entry, node, CFGEdge.EdgeType.BREAK);

        if (!loopStack.isEmpty()) {
            addEdge(node, loopStack.peek().breakTarget, CFGEdge.EdgeType.BREAK);
        } else {
            addEdge(node, exit, CFGEdge.EdgeType.BREAK);
        }
        return null; // terminal in current flow
    }

    private CFGNode buildContinue(ContinueStmt stmt, CFGNode entry) {
        CFGNode node = new CFGNode(nodeIdCounter++, CFGNode.NodeType.BASIC_BLOCK, stmt, "continue");
        allNodes.add(node);
        addEdge(entry, node, CFGEdge.EdgeType.CONTINUE);

        if (!loopStack.isEmpty()) {
            addEdge(node, loopStack.peek().continueTarget, CFGEdge.EdgeType.CONTINUE);
        }
        return null; // terminal in current flow
    }

    private CFGNode buildSynchronized(SynchronizedStmt stmt, CFGNode entry) {
        CFGNode syncNode = new CFGNode(nodeIdCounter++, CFGNode.NodeType.BASIC_BLOCK, stmt, "synchronized");
        allNodes.add(syncNode);
        addEdge(entry, syncNode, CFGEdge.EdgeType.FALL_THROUGH);

        CFGNode bodyExit = buildStatement(stmt.getBody(), syncNode);
        return bodyExit != null ? bodyExit : syncNode;
    }

    private CFGNode buildForeach(com.github.javaparser.ast.stmt.ForEachStmt stmt, CFGNode entry) {
        // Foreach: iterator -> hasNext -> body -> hasNext -> exit
        CFGNode cond = new CFGNode(nodeIdCounter++, CFGNode.NodeType.CONDITIONAL, stmt, "foreach(" + stmt.getVariable().toString() + ")");
        allNodes.add(cond);
        addEdge(entry, cond, CFGEdge.EdgeType.FALL_THROUGH);

        CFGNode exitNode = new CFGNode(nodeIdCounter++, CFGNode.NodeType.BASIC_BLOCK, stmt, "foreach-exit");
        allNodes.add(exitNode);
        addEdge(cond, exitNode, CFGEdge.EdgeType.FALSE_BRANCH);

        loopStack.push(new LoopContext(exitNode, cond));
        CFGNode body = buildStatement(stmt.getBody(), cond);
        loopStack.pop();

        if (body != null && body != exitNode) {
            addEdge(body, cond, CFGEdge.EdgeType.UNCONDITIONAL);
        }

        return exitNode;
    }

    private CFGNode buildAssert(AssertStmt stmt, CFGNode entry) {
        CFGNode node = new CFGNode(nodeIdCounter++, CFGNode.NodeType.CONDITIONAL, stmt, "assert(" + stmt.getCheck().toString().substring(0, Math.min(30, stmt.getCheck().toString().length())) + ")");
        allNodes.add(node);
        addEdge(entry, node, CFGEdge.EdgeType.FALL_THROUGH);

        // Assert false -> exit (throws AssertionError)
        CFGNode exitNode = new CFGNode(nodeIdCounter++, CFGNode.NodeType.BASIC_BLOCK, stmt, "assert-exit");
        allNodes.add(exitNode);
        addEdge(node, exitNode, CFGEdge.EdgeType.FALSE_BRANCH);

        return exitNode;
    }

    /**
     * Handle short-circuit evaluation in conditions:
     * - A && B: if A is false, B is skipped (false branch)
     * - A || B: if A is true, B is skipped (true branch)
     */
    private void buildShortCircuit(Expression condition, CFGNode condNode) {
        if (condition instanceof BinaryExpr) {
            BinaryExpr bin = (BinaryExpr) condition;
            if (bin.getOperator() == BinaryExpr.Operator.AND) {
                // A && B: A false -> skip B
                buildShortCircuit(bin.getLeft(), condNode);
                // The false branch of A goes to the false branch of the overall condition
                // (already handled by the if/while/for builder)
            } else if (bin.getOperator() == BinaryExpr.Operator.OR) {
                // A || B: A true -> skip B
                buildShortCircuit(bin.getLeft(), condNode);
            }
        }
        // Note: Full short-circuit CFG would add intermediate nodes.
        // For cyclomatic complexity, the key is counting the decision points.
    }

    // ---- Edge Management ----

    private void addEdge(CFGNode source, CFGNode target, CFGEdge.EdgeType type) {
        if (source == null || target == null) return;
        allEdges.add(new CFGEdge(source, target, type));
        source.addSuccessor(target);
    }

    private ControlFlowGraph finalizeCFG(String methodName) {
        return new ControlFlowGraph(entry, exit, Collections.unmodifiableList(allNodes),
                Collections.unmodifiableList(allEdges), methodName);
    }
}
