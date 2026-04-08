package cn.dolphinmind.glossary.java.analyze.dataflow;

import com.github.javaparser.ast.stmt.Statement;

import java.util.Collections;
import java.util.List;

/**
 * A node in the Data Flow Graph, representing a single statement.
 */
public class DFGNode {

    private final int id;
    private final Statement statement;
    private final List<String> defs;   // variables defined in this statement
    private final List<String> uses;   // variables used in this statement

    public DFGNode(int id, Statement statement, List<String> defs, List<String> uses) {
        this.id = id;
        this.statement = statement;
        this.defs = Collections.unmodifiableList(defs);
        this.uses = Collections.unmodifiableList(uses);
    }

    public int getId() { return id; }
    public Statement getStatement() { return statement; }
    public List<String> getDefs() { return defs; }
    public List<String> getUses() { return uses; }

    public String getLabel() {
        if (statement == null) return "null";
        String str = statement.toString();
        return str.length() > 60 ? str.substring(0, 60) + "..." : str;
    }

    @Override
    public String toString() {
        return "N" + id + ": " + getLabel();
    }
}
