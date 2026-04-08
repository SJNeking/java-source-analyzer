package cn.dolphinmind.glossary.java.analyze.dataflow;

/**
 * An edge in the Data Flow Graph representing a def-use chain.
 * source (def of variable) → target (use of variable)
 */
public class DFGEdge {

    private final DFGNode source;
    private final DFGNode target;
    private final String variable;

    public DFGEdge(DFGNode source, DFGNode target, String variable) {
        this.source = source;
        this.target = target;
        this.variable = variable;
    }

    public DFGNode getSource() { return source; }
    public DFGNode getTarget() { return target; }
    public String getVariable() { return variable; }

    @Override
    public String toString() {
        return source.getId() + " -[" + variable + "]-> " + target.getId();
    }
}
