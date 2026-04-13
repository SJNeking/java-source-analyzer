package cn.dolphinmind.glossary.java.analyze.dataflow;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for ControlFlowAwareDataFlowGraph and EnhancedTaintEngine.
 */
public class DataFlowGraphTest {

    @Test
    public void testControlFlowAwareDfg() throws Exception {
        String code = "void test() {\n" +
            "String id = request.getParameter(\"id\");\n" +
            "String sql = \"SELECT * FROM users WHERE id = \" + id;\n" +
            "Statement stmt = conn.createStatement();\n" +
            "stmt.executeQuery(sql);\n" +
            "}\n";

        MethodDeclaration md = StaticJavaParser.parseMethodDeclaration(code);
        ControlFlowAwareDataFlowGraph dfg = new ControlFlowAwareDataFlowGraph(md);

        // Should have nodes for each statement
        assertTrue("Should have nodes", dfg.getNodes().size() > 0);
        System.out.println("DFG Nodes: " + dfg.getNodes().size());
        for (DFGNode node : dfg.getNodes()) {
            System.out.println("  Node " + node.getId() + ": defs=" + node.getDefs() + ", uses=" + node.getUses());
        }
        System.out.println("Edges: " + dfg.getEdges().size());
        for (DFGEdge edge : dfg.getEdges()) {
            System.out.println("  Edge: N" + edge.getSource().getId() + " -> N" + edge.getTarget().getId() + " via " + edge.getVariable());
        }
    }

    @Test
    public void testObjectCreationDetection() throws Exception {
        String code = "void readFile() {\n" +
            "String filename = request.getParameter(\"file\");\n" +
            "FileInputStream fis = new FileInputStream(filename);\n" +
            "}\n";

        MethodDeclaration md = StaticJavaParser.parseMethodDeclaration(code);

        // Check if ObjectCreationExpr is found
        List<com.github.javaparser.ast.expr.ObjectCreationExpr> creations =
            md.getBody().get().findAll(com.github.javaparser.ast.expr.ObjectCreationExpr.class);
        System.out.println("ObjectCreationExpr count: " + creations.size());
        for (com.github.javaparser.ast.expr.ObjectCreationExpr oce : creations) {
            System.out.println("  Type: " + oce.getType().asString());
            System.out.println("  Args: " + oce.getArguments());
            System.out.println("  Full: " + oce.toString());
        }

        // Check source patterns
        String srcPattern = "getParameter";
        boolean hasSource = md.toString().contains(srcPattern);
        System.out.println("Has source pattern: " + hasSource);

        // Check sink patterns
        String[] sinkPatterns = {"FileInputStream", "File", "Paths.get"};
        for (String p : sinkPatterns) {
            boolean found = md.toString().contains(p);
            System.out.println("Sink pattern '" + p + "': " + found);
        }
    }
}
