package cn.dolphinmind.glossary.java.analyze.parser;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for SQL script files
 */
public class SqlFileParser implements FileParser {

    @Override
    public boolean supports(Path file) {
        return file.getFileName().toString().toLowerCase().endsWith(".sql");
    }

    @Override
    public List<FileAsset> parse(Path file, String projectRoot) {
        List<FileAsset> assets = new ArrayList<>();
        try {
            String content = String.join("\n", Files.readAllLines(file, StandardCharsets.UTF_8));

            FileAsset asset = new FileAsset(
                    relativize(file, projectRoot).toString(),
                    FileAsset.AssetType.SQL_SCRIPT
            );

            // Extract CREATE TABLE statements
            List<Map<String, Object>> tables = extractTables(content);
            asset.putMeta("tables", tables);
            asset.putMeta("table_count", tables.size());

            // Extract INSERT statements count
            int insertCount = countPattern(content, "INSERT\\s+INTO");
            asset.putMeta("insert_count", insertCount);

            // Extract ALTER TABLE statements
            int alterCount = countPattern(content, "ALTER\\s+TABLE");
            asset.putMeta("alter_count", alterCount);

            // Extract CREATE INDEX statements
            List<String> indexes = extractIndexes(content);
            asset.putMeta("indexes", indexes);

            // Extract all table names referenced
            List<String> allTableRefs = extractAllTableReferences(content);
            asset.putMeta("table_references", allTableRefs);

            // Content preview
            asset.putMeta("content_preview", content.length() > 1000 ? content.substring(0, 1000) + "..." : content);
            asset.putMeta("line_count", content.split("\n").length);
            asset.putMeta("file_size_bytes", content.length());

            assets.add(asset);
        } catch (Exception e) {
            FileAsset asset = new FileAsset(
                    relativize(file, projectRoot).toString(),
                    FileAsset.AssetType.SQL_SCRIPT
            );
            asset.putMeta("error", e.getMessage());
            assets.add(asset);
        }
        return assets;
    }

    @Override
    public FileAsset.AssetType getAssetType() {
        return FileAsset.AssetType.SQL_SCRIPT;
    }

    private List<Map<String, Object>> extractTables(String sql) {
        List<Map<String, Object>> tables = new ArrayList<>();
        Pattern createPattern = Pattern.compile(
                "CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(?:`?(\\w+)`?\\.)?`?(\\w+)`?\\s*\\((.*?)\\);",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = createPattern.matcher(sql);

        while (matcher.find()) {
            Map<String, Object> table = new LinkedHashMap<>();
            String schema = matcher.group(1);
            String tableName = matcher.group(2);
            String columns = matcher.group(3);

            table.put("schema", schema != null ? schema : "");
            table.put("table_name", tableName);

            // Extract column definitions
            List<String> columnNames = new ArrayList<>();
            String[] lines = columns.split(",");
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("PRIMARY") || line.startsWith("KEY") || line.startsWith("INDEX") ||
                    line.startsWith("UNIQUE") || line.startsWith("CONSTRAINT") || line.startsWith("FOREIGN")) {
                    continue;
                }
                // Extract column name (first word, possibly quoted)
                Pattern colPattern = Pattern.compile("`?(\\w+)`?\\s+\\w+");
                Matcher colMatcher = colPattern.matcher(line);
                if (colMatcher.find()) {
                    columnNames.add(colMatcher.group(1));
                }
            }
            table.put("columns", columnNames);
            table.put("column_count", columnNames.size());

            tables.add(table);
        }
        return tables;
    }

    private List<String> extractIndexes(String sql) {
        List<String> indexes = new ArrayList<>();
        Pattern indexPattern = Pattern.compile(
                "CREATE\\s+(?:UNIQUE\\s+)?INDEX\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?`?(\\w+)`?\\s+ON\\s+`?(\\w+)`?",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = indexPattern.matcher(sql);
        while (matcher.find()) {
            String indexName = matcher.group(1);
            String tableName = matcher.group(2);
            indexes.add(indexName + " ON " + tableName);
        }
        return indexes;
    }

    private List<String> extractAllTableReferences(String sql) {
        List<String> tables = new ArrayList<>();
        // Match FROM, JOIN, INTO, TABLE keywords followed by table names
        Pattern tableRefPattern = Pattern.compile(
                "(?:FROM|JOIN|INTO|UPDATE|TABLE)\\s+`?(\\w+)`?",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = tableRefPattern.matcher(sql);
        while (matcher.find()) {
            String tableName = matcher.group(1);
            if (!tables.contains(tableName)) {
                tables.add(tableName);
            }
        }
        return tables;
    }

    private int countPattern(String sql, String regex) {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sql);
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }
}
