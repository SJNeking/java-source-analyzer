package cn.dolphinmind.glossary.java.analyze.relation;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cross-file relationship engine
 * Discovers relationships between Java classes, MyBatis mappers, SQL tables, config items, etc.
 */
public class RelationEngine {

    private final List<AssetRelation> relations = new ArrayList<>();

    /**
     * Discover all cross-file relationships from scanned project data
     */
    public List<AssetRelation> discoverRelations(Map<String, Object> javaAssets,
                                                    Map<String, Object> projectAssets) {
        // 1. Java ↔ MyBatis Mapper
        discoverJavaToMapperRelations(javaAssets, projectAssets);

        // 2. SQL Table ↔ Entity Class
        discoverSqlToEntityRelations(javaAssets, projectAssets);

        // 3. Config → Class (middleware detection)
        discoverConfigToClassRelations(projectAssets);

        // 4. POM Dependency → Usage
        discoverPomToUsageRelations(javaAssets, projectAssets);

        // 5. Dockerfile → JAR
        discoverDockerToJarRelations(javaAssets, projectAssets);

        return Collections.unmodifiableList(relations);
    }

    /**
     * Java interface ↔ MyBatis Mapper XML (via namespace matching)
     */
    private void discoverJavaToMapperRelations(Map<String, Object> javaAssets,
                                                  Map<String, Object> projectAssets) {
        // Collect all Java class addresses
        List<String> javaClasses = extractJavaClassAddresses(javaAssets);

        // Collect all MyBatis mapper namespaces
        List<Map<String, String>> mappers = extractMapperNamespaces(projectAssets);

        for (Map<String, String> mapper : mappers) {
            String namespace = mapper.get("namespace");
            String mapperPath = mapper.get("path");

            // Match namespace to Java class
            for (String javaClass : javaClasses) {
                if (javaClass.equals(namespace) || javaClass.endsWith("." + namespace)) {
                    relations.add(new AssetRelation(
                            mapperPath, "Mapper: " + namespace,
                            "", "Java: " + javaClass,
                            AssetRelation.RelationType.JAVA_TO_MAPPER,
                            "namespace=\"" + namespace + "\"",
                            0.95
                    ));
                    break;
                }
            }
        }
    }

    /**
     * SQL Table → Java Entity Class (via table name ↔ class name heuristics)
     */
    private void discoverSqlToEntityRelations(Map<String, Object> javaAssets,
                                                 Map<String, Object> projectAssets) {
        List<String> sqlTables = extractSqlTables(projectAssets);
        List<String> javaClasses = extractJavaClassAddresses(javaAssets);

        for (String table : sqlTables) {
            String expectedClassName = toClassName(table);
            for (String javaClass : javaClasses) {
                String simpleName = javaClass.substring(javaClass.lastIndexOf('.') + 1);
                if (simpleName.equalsIgnoreCase(expectedClassName) ||
                    simpleName.toLowerCase().contains(table.toLowerCase().replaceAll("_", ""))) {
                    relations.add(new AssetRelation(
                            "", "SQL Table: " + table,
                            "", "Java Entity: " + javaClass,
                            AssetRelation.RelationType.SQL_TO_ENTITY,
                            "Name match: " + table + " → " + simpleName,
                            0.7
                    ));
                }
            }
        }
    }

    /**
     * Config → Class (middleware detection from application.yml)
     */
    private void discoverConfigToClassRelations(Map<String, Object> projectAssets) {
        List<Map<String, Object>> configs = extractConfigAssets(projectAssets);
        for (Map<String, Object> config : configs) {
            @SuppressWarnings("unchecked")
            List<String> middleware = (List<String>) config.getOrDefault("middleware", Collections.emptyList());
            for (String mw : middleware) {
                relations.add(new AssetRelation(
                        config.get("path").toString(), "Config: " + config.get("file_name"),
                        "", "Framework: " + mw,
                        AssetRelation.RelationType.CONFIG_TO_CLASS,
                        "Detected middleware: " + mw,
                        0.8
                ));
            }
        }
    }

    /**
     * POM Dependency → Actual Usage (check if classes from dependency are imported)
     */
    private void discoverPomToUsageRelations(Map<String, Object> javaAssets,
                                                Map<String, Object> projectAssets) {
        List<Map<String, String>> deps = extractPomDependencies(projectAssets);
        for (Map<String, String> dep : deps) {
            String artifactId = dep.get("artifactId");
            if (artifactId != null && !artifactId.isEmpty()) {
                relations.add(new AssetRelation(
                        "pom.xml", "Dependency: " + dep.get("groupId") + ":" + artifactId,
                        "", "Project Code",
                        AssetRelation.RelationType.POM_TO_USAGE,
                        "Declared dependency: " + artifactId,
                        0.5
                ));
            }
        }
    }

    /**
     * Dockerfile → Startup JAR
     */
    private void discoverDockerToJarRelations(Map<String, Object> javaAssets,
                                                 Map<String, Object> projectAssets) {
        List<Map<String, Object>> dockerfiles = extractDockerfiles(projectAssets);
        for (Map<String, Object> dockerfile : dockerfiles) {
            String jarPath = (String) dockerfile.getOrDefault("jar_path", "");
            if (!jarPath.isEmpty()) {
                relations.add(new AssetRelation(
                        dockerfile.get("path").toString(), "Dockerfile",
                        "", "JAR: " + jarPath,
                        AssetRelation.RelationType.DOCKER_TO_JAR,
                        "Referenced JAR: " + jarPath,
                        0.85
                ));
            }
        }
    }

    // --- Extraction Helpers ---

    @SuppressWarnings("unchecked")
    private List<String> extractJavaClassAddresses(Map<String, Object> javaAssets) {
        List<String> addresses = new ArrayList<>();
        Object assetsObj = javaAssets.get("assets");
        if (assetsObj instanceof List) {
            for (Object asset : (List<Object>) assetsObj) {
                if (asset instanceof Map) {
                    Object addr = ((Map<Object, Object>) asset).get("address");
                    if (addr != null) addresses.add(addr.toString());
                }
            }
        }
        return addresses;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> extractMapperNamespaces(Map<String, Object> projectAssets) {
        List<Map<String, String>> result = new ArrayList<>();
        Object mappersObj = projectAssets.get("mybatis_mapper");
        if (mappersObj instanceof List) {
            for (Object obj : (List<Object>) mappersObj) {
                if (obj instanceof Map) {
                    Map<Object, Object> mapper = (Map<Object, Object>) obj;
                    String ns = (String) mapper.get("namespace");
                    String path = (String) mapper.get("path");
                    if (ns != null && !ns.isEmpty()) {
                        Map<String, String> entry = new LinkedHashMap<>();
                        entry.put("namespace", ns);
                        entry.put("path", path);
                        result.add(entry);
                    }
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<String> extractSqlTables(Map<String, Object> projectAssets) {
        List<String> tables = new ArrayList<>();
        Object sqlObj = projectAssets.get("sql_script");
        if (sqlObj instanceof List) {
            for (Object obj : (List<Object>) sqlObj) {
                if (obj instanceof Map) {
                    Map<Object, Object> sql = (Map<Object, Object>) obj;
                    Object tablesObj = sql.get("tables");
                    if (tablesObj instanceof List) {
                        for (Object t : (List<Object>) tablesObj) {
                            if (t instanceof Map) {
                                Object tableName = ((Map<Object, Object>) t).get("table_name");
                                if (tableName != null) tables.add(tableName.toString());
                            }
                        }
                    }
                }
            }
        }
        return tables;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractConfigAssets(Map<String, Object> projectAssets) {
        List<Map<String, Object>> result = new ArrayList<>();
        Object yamlObj = projectAssets.get("yaml_config");
        if (yamlObj instanceof List) {
            for (Object obj : (List<Object>) yamlObj) {
                if (obj instanceof Map) {
                    result.add((Map<String, Object>) obj);
                }
            }
        }
        Object propsObj = projectAssets.get("properties_config");
        if (propsObj instanceof List) {
            for (Object obj : (List<Object>) propsObj) {
                if (obj instanceof Map) {
                    result.add((Map<String, Object>) obj);
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> extractPomDependencies(Map<String, Object> projectAssets) {
        List<Map<String, String>> result = new ArrayList<>();
        Object pomObj = projectAssets.get("maven_pom");
        if (pomObj instanceof List) {
            for (Object obj : (List<Object>) pomObj) {
                if (obj instanceof Map) {
                    Map<Object, Object> pom = (Map<Object, Object>) obj;
                    Object depsObj = pom.get("dependencies");
                    if (depsObj instanceof List) {
                        for (Object dep : (List<Object>) depsObj) {
                            if (dep instanceof Map) {
                                result.add((Map<String, String>) dep);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractDockerfiles(Map<String, Object> projectAssets) {
        List<Map<String, Object>> result = new ArrayList<>();
        Object dockerObj = projectAssets.get("dockerfile");
        if (dockerObj instanceof List) {
            for (Object obj : (List<Object>) dockerObj) {
                if (obj instanceof Map) {
                    result.add((Map<String, Object>) obj);
                }
            }
        }
        return result;
    }

    /**
     * Convert snake_case table name to CamelCase class name
     * e.g., user_order → UserOrder
     */
    private String toClassName(String tableName) {
        String[] parts = tableName.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) sb.append(part.substring(1));
            }
        }
        return sb.toString();
    }

    public List<AssetRelation> getRelations() {
        return relations;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total_relations", relations.size());

        Map<String, List<Map<String, Object>>> byType = new LinkedHashMap<>();
        for (AssetRelation rel : relations) {
            byType.computeIfAbsent(rel.getType().name(), k -> new ArrayList<>()).add(rel.toMap());
        }
        result.put("relations_by_type", byType);
        result.put("relations", relations.stream().map(AssetRelation::toMap).toArray());

        return result;
    }
}
