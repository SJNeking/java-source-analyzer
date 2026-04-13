package cn.dolphinmind.glossary.java.analyze.quality.rules;

import cn.dolphinmind.glossary.java.analyze.quality.AbstractLoggingRule;
import cn.dolphinmind.glossary.java.analyze.quality.QualityIssue;
import cn.dolphinmind.glossary.java.analyze.quality.Severity;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.type.Type;

import java.util.*;

/**
 * RSPEC-20024: Record should not contain mutable fields.
 *
 * Records are designed to be immutable data carriers. If a record contains
 * mutable fields (arrays, collections, mutable objects), it breaks the
 * immutability contract and can lead to subtle bugs.
 *
 * Detects:
 * - Record with array fields (e.g., record User(String[] tags))
 * - Record with collection fields (e.g., record User(List<String> permissions))
 * - Record with mutable object fields (e.g., record Config(Date created))
 */
public class RecordMutableFieldsRule extends AbstractLoggingRule<CompilationUnit> {

    @Override
    public String getRuleKey() { return "RSPEC-20024"; }

    @Override
    public String getName() { return "Record should not contain mutable fields"; }

    @Override
    public String getCategory() { return "CODE_SMELL"; }

    @Override
    @SuppressWarnings("unchecked")
    protected List<QualityIssue> doCheck(Map<String, Object> classAsset) {
        List<QualityIssue> issues = new ArrayList<>();
        String className = (String) classAsset.getOrDefault("address", "");
        String filePath = (String) classAsset.getOrDefault("source_file", "");
        String kind = (String) classAsset.getOrDefault("kind", "");

        // Only analyze records
        if (!"RECORD".equalsIgnoreCase(kind) && !className.toLowerCase().contains("record")) {
            return issues;
        }

        // Check fields for mutability
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fields = (List<Map<String, Object>>)
            classAsset.getOrDefault("fields_matrix", Collections.emptyList());

        for (Map<String, Object> field : fields) {
            String fieldName = (String) field.getOrDefault("name", "");
            String fieldType = (String) field.getOrDefault("type", "");

            String mutableType = checkIfMutableType(fieldType);
            if (mutableType != null) {
                int line = (int) field.getOrDefault("line", 0);
                issues.add(new QualityIssue.Builder()
                    .ruleKey("RSPEC-20024")
                    .ruleName("Record should not contain mutable fields")
                    .severity(Severity.MAJOR)
                    .category("CODE_SMELL")
                    .filePath(filePath)
                    .className(className)
                    .methodName("")
                    .line(line)
                    .message("Record field '" + fieldName + "' has mutable type '" + mutableType +
                        "' - breaks immutability contract")
                    .evidence(fieldType + " " + fieldName)
                    .build());
            }
        }

        return issues;
    }

    /**
     * Check if a type is mutable.
     * Returns the mutable type name if mutable, null otherwise.
     */
    private String checkIfMutableType(String type) {
        if (type == null) return null;

        // Arrays are mutable
        if (type.contains("[]") || type.contains("Array")) {
            return "array";
        }

        // Collections are mutable
        if (type.contains("List") || type.contains("Set") || type.contains("Map") ||
            type.contains("Collection") || type.contains("ArrayList") ||
            type.contains("HashSet") || type.contains("HashMap")) {
            return "collection";
        }

        // StringBuilder/StringBuffer are mutable
        if (type.contains("StringBuilder") || type.contains("StringBuffer")) {
            return "mutable string";
        }

        // Date is mutable
        if (type.equals("Date") || type.contains("java.util.Date")) {
            return "java.util.Date";
        }

        // Calendar is mutable
        if (type.contains("Calendar")) {
            return "Calendar";
        }

        return null;
    }
}
