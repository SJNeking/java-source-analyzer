package cn.dolphinmind.glossary.java.analyze.quality.rules;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests for COLLECTION category rules.
 */
public class CollectionRulesTest extends AbstractRuleTest {

    @Test
    public void arrayListAsQueue_shouldDetect() {
        CollectionRules.ArrayListAsQueue rule = new CollectionRules.ArrayListAsQueue();
        Map<String, Object> method = createMethod("test", "List<String> list = new ArrayList<>();\n" +
            "list.remove(0);\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void hashMapInitialCapacity_shouldDetect() {
        CollectionRules.HashMapInitialCapacity rule = new CollectionRules.HashMapInitialCapacity();
        Map<String, Object> method = createMethod("test", "Map<String, String> map = new HashMap<>();\n" +
            "map.put(\"a\", \"1\");\n" +
            "map.put(\"b\", \"2\");\n" +
            "map.put(\"c\", \"3\");\n" +
            "map.put(\"d\", \"4\");\n" +
            "map.put(\"e\", \"5\");\n" +
            "map.put(\"f\", \"6\");\n" +
            "map.put(\"g\", \"7\");\n" +
            "map.put(\"h\", \"8\");\n" +
            "map.put(\"i\", \"9\");\n" +
            "map.put(\"j\", \"10\");\n" +
            "map.put(\"k\", \"11\");\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void linkedListRandomAccess_shouldDetect() {
        CollectionRules.LinkedListRandomAccess rule = new CollectionRules.LinkedListRandomAccess();
        Map<String, Object> method = createMethod("test", "List<String> list = new LinkedList<>();\n" +
            "return list.get(0);\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void treeMapWhenNotNeeded_shouldDetect() {
        CollectionRules.TreeMapWhenNotNeeded rule = new CollectionRules.TreeMapWhenNotNeeded();
        Map<String, Object> method = createMethod("test", "Map<String, String> map = new TreeMap<>();\n" +
            "map.put(\"key\", \"value\");\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void enumerationInsteadOfIterator_shouldDetect() {
        CollectionRules.EnumerationInsteadOfIterator rule = new CollectionRules.EnumerationInsteadOfIterator();
        Map<String, Object> method = createMethod("test", "Enumeration<String> e = vector.elements();\n");
        assertIssues(rule, method, 1);
    }
}
