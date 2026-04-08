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
 * Parser for MyBatis Mapper XML files
 */
public class MyBatisXmlParser implements FileParser {

    @Override
    public boolean supports(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        return name.endsWith("mapper.xml") || name.endsWith("mapping.xml") ||
               name.endsWith("dao.xml") || name.endsWith("-mapper.xml") ||
               name.endsWith("-dao.xml") ||
               (name.endsWith(".xml") && file.getParent() != null &&
                (file.getParent().toString().contains("mapper") || file.getParent().toString().contains("mybatis")));
    }

    @Override
    public List<FileAsset> parse(Path file, String projectRoot) {
        List<FileAsset> assets = new ArrayList<>();
        try {
            String content = String.join("\n", Files.readAllLines(file, StandardCharsets.UTF_8));

            FileAsset asset = new FileAsset(
                    relativize(file, projectRoot).toString(),
                    FileAsset.AssetType.MYBATIS_MAPPER
            );

            // Extract namespace
            String namespace = extractXmlAttribute(content, "namespace");
            asset.putMeta("namespace", namespace);

            // Extract mapper interface name
            String mapperInterface = namespace;
            asset.putMeta("mapper_interface", mapperInterface);

            // Extract SQL statements
            List<Map<String, Object>> statements = extractStatements(content);
            asset.putMeta("statements", statements);
            asset.putMeta("statement_count", statements.size());

            // Extract resultMap definitions
            List<Map<String, Object>> resultMaps = extractResultMaps(content);
            asset.putMeta("result_maps", resultMaps);
            asset.putMeta("result_map_count", resultMaps.size());

            // Extract referenced tables
            List<String> tables = extractTableReferences(content);
            asset.putMeta("tables", tables);

            // Extract SQL fragments
            int sqlFragmentCount = countTag(content, "sql");
            asset.putMeta("sql_fragments", sqlFragmentCount);

            asset.putMeta("line_count", content.split("\n").length);

            assets.add(asset);
        } catch (Exception e) {
            FileAsset asset = new FileAsset(
                    relativize(file, projectRoot).toString(),
                    FileAsset.AssetType.MYBATIS_MAPPER
            );
            asset.putMeta("error", e.getMessage());
            assets.add(asset);
        }
        return assets;
    }

    @Override
    public FileAsset.AssetType getAssetType() {
        return FileAsset.AssetType.MYBATIS_MAPPER;
    }

    private String extractXmlAttribute(String xml, String attrName) {
        Pattern pattern = Pattern.compile(attrName + "\\s*=\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(xml);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    private List<Map<String, Object>> extractStatements(String content) {
        List<Map<String, Object>> statements = new ArrayList<>();

        // Extract <select> statements
        extractStatementType(content, "select", statements);
        extractStatementType(content, "insert", statements);
        extractStatementType(content, "update", statements);
        extractStatementType(content, "delete", statements);

        return statements;
    }

    private void extractStatementType(String content, String tag, List<Map<String, Object>> statements) {
        Pattern pattern = Pattern.compile(
                "<" + tag + "\\s+([^>]+)>(.*?)</" + tag + ">",
                Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            Map<String, Object> stmt = new LinkedHashMap<>();
            String attrs = matcher.group(1);
            String sql = matcher.group(2).trim();

            // Extract id
            Pattern idPattern = Pattern.compile("id\\s*=\\s*\"([^\"]+)\"");
            Matcher idMatcher = idPattern.matcher(attrs);
            if (idMatcher.find()) {
                stmt.put("id", idMatcher.group(1));
            }

            // Extract parameterType
            Pattern paramPattern = Pattern.compile("parameterType\\s*=\\s*\"([^\"]+)\"");
            Matcher paramMatcher = paramPattern.matcher(attrs);
            if (paramMatcher.find()) {
                stmt.put("parameter_type", paramMatcher.group(1));
            }

            // Extract resultType or resultMap
            Pattern resultPattern = Pattern.compile("(?:resultType|resultMap)\\s*=\\s*\"([^\"]+)\"");
            Matcher resultMatcher = resultPattern.matcher(attrs);
            if (resultMatcher.find()) {
                stmt.put("result_type", resultMatcher.group(1));
            }

            stmt.put("type", tag.toUpperCase());
            stmt.put("sql_preview", sql.length() > 200 ? sql.substring(0, 200) + "..." : sql);
            stmt.put("sql_length", sql.length());

            statements.add(stmt);
        }
    }

    private List<Map<String, Object>> extractResultMaps(String content) {
        List<Map<String, Object>> resultMaps = new ArrayList<>();
        Pattern pattern = Pattern.compile(
                "<resultMap\\s+([^>]+)>(.*?)</resultMap>",
                Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            Map<String, Object> rm = new LinkedHashMap<>();
            String attrs = matcher.group(1);
            String body = matcher.group(2);

            Pattern idPattern = Pattern.compile("id\\s*=\\s*\"([^\"]+)\"");
            Matcher idMatcher = idPattern.matcher(attrs);
            if (idMatcher.find()) {
                rm.put("id", idMatcher.group(1));
            }

            Pattern typePattern = Pattern.compile("type\\s*=\\s*\"([^\"]+)\"");
            Matcher typeMatcher = typePattern.matcher(attrs);
            if (typeMatcher.find()) {
                rm.put("type", typeMatcher.group(1));
            }

            // Count field mappings
            int resultCount = countOccurrences(body, "<result");
            int idCount = countOccurrences(body, "<id");
            rm.put("result_count", resultCount);
            rm.put("id_mapping_count", idCount);

            resultMaps.add(rm);
        }
        return resultMaps;
    }

    private List<String> extractTableReferences(String content) {
        List<String> tables = new ArrayList<>();
        // Match FROM, INTO, UPDATE, JOIN followed by table names
        Pattern pattern = Pattern.compile(
                "(?:FROM|INTO|UPDATE|JOIN)\\s+`?([a-zA-Z_]\\w*)`?",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            String tableName = matcher.group(1);
            if (!tables.contains(tableName)) {
                tables.add(tableName);
            }
        }
        return tables;
    }

    private int countTag(String content, String tag) {
        Pattern pattern = Pattern.compile("<" + tag + "[\\s>]");
        Matcher matcher = pattern.matcher(content);
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }

    private int countOccurrences(String content, String substring) {
        int count = 0;
        int idx = 0;
        while ((idx = content.indexOf(substring, idx)) != -1) {
            count++;
            idx += substring.length();
        }
        return count;
    }
}
