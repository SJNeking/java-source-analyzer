package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.Severity;

import java.util.*;
import java.util.regex.*;

/**
 * Collection Rules
 *
 * Detects collection anti-patterns:
 * - Wrong collection type
 * - Initial capacity not set
 * - Inefficient operations
 * - Type safety issues
 * - Concurrent modification
 * - Memory overhead
 */
public final class CollectionRules {
    private CollectionRules() {}

    /**
     * RSPEC-16001: ArrayList used as queue
     */
    public static class ArrayListAsQueue extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-16001"; }
        public String getName() { return "Use Queue instead of ArrayList for queue operations"; }
        public String getCategory() { return "COLLECTION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("new ArrayList<>()") && (body.contains(".remove(0)") || body.contains(".get(0)"))) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "ArrayList used as queue", "use Queue/LinkedList"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-16002: HashSet with initial capacity
     */
    public static class HashSetInitialCapacity extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-16002"; }
        public String getName() { return "Set initial capacity for HashSet"; }
        public String getCategory() { return "COLLECTION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("new HashSet<>()") && body.contains(".add(") && body.split("\\.add\\(").length > 10) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "HashSet without initial capacity", "performance"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-16003: HashMap with initial capacity
     */
    public static class HashMapInitialCapacity extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-16003"; }
        public String getName() { return "Set initial capacity for HashMap"; }
        public String getCategory() { return "COLLECTION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("new HashMap<>()") && body.contains(".put(") && body.split("\\.put\\(").length > 10) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "HashMap without initial capacity", "performance"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-16004: LinkedList used for random access
     */
    public static class LinkedListRandomAccess extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-16004"; }
        public String getName() { return "Use ArrayList instead of LinkedList for random access"; }
        public String getCategory() { return "COLLECTION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("new LinkedList<>()") && (body.contains(".get(") || body.contains("["))) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "LinkedList random access", "O(n) vs O(1)"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-16005: Concurrent modification
     */
    public static class ConcurrentModification extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-16005"; }
        public String getName() { return "Do not modify collection while iterating"; }
        public String getCategory() { return "COLLECTION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("for (") && body.contains(".iterator(") && body.contains(".remove(")) {
                if (!body.contains("Iterator") && !body.contains(".remove()") && body.contains(".remove(")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                        "Concurrent modification", "use Iterator.remove()"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-16006: Collections.singleton for mutable set
     */
    public static class MutableSingletonSet extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-16006"; }
        public String getName() { return "Collections.singleton returns immutable set"; }
        public String getCategory() { return "COLLECTION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("Collections.singleton") && (body.contains(".add(") || body.contains(".remove("))) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Modification of singleton set", "UnsupportedOperationException"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-16007: Arrays.asList returns fixed-size list
     */
    public static class FixedSizeList extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-16007"; }
        public String getName() { return "Arrays.asList returns fixed-size list"; }
        public String getCategory() { return "COLLECTION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("Arrays.asList") && (body.contains(".add(") || body.contains(".remove("))) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                    "Modification of Arrays.asList", "UnsupportedOperationException"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-16008: TreeMap instead of HashMap when ordering not needed
     */
    public static class TreeMapWhenNotNeeded extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-16008"; }
        public String getName() { return "Use HashMap instead of TreeMap when ordering not needed"; }
        public String getCategory() { return "COLLECTION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("new TreeMap<>()") && !body.contains(".firstKey()") && !body.contains(".lastKey()") &&
                !body.contains("subMap") && !body.contains("headMap") && !body.contains("tailMap")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "TreeMap without ordering operations", "use HashMap for O(1)"));
            }
            return issues;
        }
    }

    /**
     * RSPEC-16009: Contains on list instead of set
     */
    public static class ContainsOnList extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-16009"; }
        public String getName() { return "Use Set instead of List for contains operations"; }
        public String getCategory() { return "COLLECTION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("List<") && body.contains(".contains(") && body.split("\\.contains\\(").length > 5) {
                if (!body.contains("Set<") && !body.contains("HashSet")) {
                    issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MAJOR, getCategory(), fp, cn, name, line,
                        "List.contains() called frequently", "use Set for O(1)"));
                }
            }
            return issues;
        }
    }

    /**
     * RSPEC-16010: Enumeration instead of Iterator
     */
    public static class EnumerationInsteadOfIterator extends AbstractMethodRule {
        public String getRuleKey() { return "RSPEC-16010"; }
        public String getName() { return "Use Iterator instead of Enumeration"; }
        public String getCategory() { return "COLLECTION"; }
        protected List<QualityIssue> checkMethod(Map<String, Object> m, String fp, String cn) {
            List<QualityIssue> issues = new ArrayList<>();
            String body = (String) m.getOrDefault("body_code", "");
            String name = (String) m.get("name");
            int line = (int) m.getOrDefault("line_start", 0);
            if (body.contains("Enumeration<") || body.contains(".elements()")) {
                issues.add(new QualityIssue(getRuleKey(), getName(), Severity.MINOR, getCategory(), fp, cn, name, line,
                    "Legacy Enumeration used", "use Iterator"));
            }
            return issues;
        }
    }
}
