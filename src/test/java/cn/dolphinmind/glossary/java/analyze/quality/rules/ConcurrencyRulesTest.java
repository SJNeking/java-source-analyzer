package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests for CONCURRENCY category rules.
 */
public class ConcurrencyRulesTest extends AbstractRuleTest {

    @Test
    public void waitNotifyNoSync_shouldDetect() {
        AllRules.WaitNotifyNoSync rule = new AllRules.WaitNotifyNoSync();
        Map<String, Object> method = createMethod("test", "obj.wait();\n", "void", null);
        assertIssues(rule, method, 1);
    }

    @Test
    public void waitNotifyNoSync_shouldNotDetectInSync() {
        AllRules.WaitNotifyNoSync rule = new AllRules.WaitNotifyNoSync();
        Map<String, Object> method = createMethod("test", "obj.wait();\n", "void", Arrays.asList("public", "synchronized"));
        assertIssues(rule, method, 0);
    }

    @Test
    public void threadRunDirect_shouldDetect() {
        AllRules.ThreadRunDirect rule = new AllRules.ThreadRunDirect();
        Map<String, Object> method = createMethod("test", "Thread t = new Thread();\nt.run();\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void threadRunDirect_shouldNotDetectWithStart() {
        AllRules.ThreadRunDirect rule = new AllRules.ThreadRunDirect();
        Map<String, Object> method = createMethod("test", "Thread t = new Thread();\nt.start();\n");
        assertIssues(rule, method, 0);
    }

    @Test
    public void nullDereference_shouldDetect() {
        AllRules.NullDereference rule = new AllRules.NullDereference();
        Map<String, Object> method = createMethod("test", "String s = null;\ns.length();\n");
        assertIssues(rule, method, 1);
    }

    @Test
    public void nullDereference_shouldNotDetectWithCheck() {
        AllRules.NullDereference rule = new AllRules.NullDereference();
        Map<String, Object> method = createMethod("test", "String s = null;\n" +
            "if (s != null) { s.length(); }\n");
        assertIssues(rule, method, 0);
    }

    @Test
    public void mutableMembersReturned_shouldDetect() {
        AllRules.MutableMembersReturned rule = new AllRules.MutableMembersReturned();
        Map<String, Object> method = createMethod("getList", "return items;\n", "java.util.List", null);
        assertIssues(rule, method, 1);
    }

    @Test
    public void mutableMembersReturned_shouldNotDetectWithCopy() {
        AllRules.MutableMembersReturned rule = new AllRules.MutableMembersReturned();
        Map<String, Object> method = createMethod("getList", "return new ArrayList<>(items);\n", "java.util.List", null);
        assertIssues(rule, method, 0);
    }
}
