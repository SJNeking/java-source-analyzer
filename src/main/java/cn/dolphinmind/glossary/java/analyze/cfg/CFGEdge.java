package cn.dolphinmind.glossary.java.analyze.cfg;

/**
 * An edge in the Control Flow Graph representing a possible transfer of control.
 */
public class CFGEdge {

    public enum EdgeType {
        /** Normal fall-through to next statement */
        FALL_THROUGH,
        /** True branch of a conditional (if true, while true, for condition true) */
        TRUE_BRANCH,
        /** False branch of a conditional */
        FALSE_BRANCH,
        /** Unconditional jump (e.g., after loop body back to condition) */
        UNCONDITIONAL,
        /** Exception thrown from try block to catch */
        EXCEPTION,
        /** Break statement exit */
        BREAK,
        /** Continue statement jump back to loop condition */
        CONTINUE,
        /** Return from method */
        RETURN,
        /** Throwable rethrown */
        THROW
    }

    private final CFGNode source;
    private final CFGNode target;
    private final EdgeType type;

    public CFGEdge(CFGNode source, CFGNode target, EdgeType type) {
        this.source = source;
        this.target = target;
        this.type = type;
    }

    public CFGNode getSource() { return source; }
    public CFGNode getTarget() { return target; }
    public EdgeType getType() { return type; }

    @Override
    public String toString() {
        return source.getId() + " -[" + type + "]-> " + target.getId();
    }
}
