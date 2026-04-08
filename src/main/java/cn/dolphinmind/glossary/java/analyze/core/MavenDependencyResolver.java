package cn.dolphinmind.glossary.java.analyze.core;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.*;

/**
 * Maven Dependency Resolver
 *
 * Parses Maven POM files with full inheritance:
 * - <parent> → resolve parent POM
 * - <dependencyManagement> → get managed versions
 * - <dependencies> → resolve actual dependencies
 * - <properties> → resolve ${property} references
 *
 * Returns all jar paths from ~/.m2/repository for the project's dependencies.
 */
public class MavenDependencyResolver {

    private final Path projectRoot;
    private final Path m2Repo;
    private final Map<String, String> properties = new HashMap<>();
    private final Map<String, String> dependencyVersions = new HashMap<>();
    private final Set<String> resolvedJars = new LinkedHashSet<>();

    public MavenDependencyResolver(Path projectRoot) {
        this.projectRoot = projectRoot;
        this.m2Repo = Paths.get(System.getProperty("user.home"), ".m2", "repository");
    }

    /**
     * Resolve all dependency jars for the project.
     * Returns jar file paths.
     */
    public Set<Path> resolveJars() throws IOException {
        // Step 1: Parse all relevant POMs to get dependencies
        Map<String, String> deps = parseAllPoms();

        // Step 2: Convert to jar paths
        for (Map.Entry<String, String> entry : deps.entrySet()) {
            String gav = entry.getKey(); // groupId:artifactId
            String version = entry.getValue();
            String[] parts = gav.split(":");
            if (parts.length == 2) {
                resolveJar(parts[0], parts[1], version);
            }
        }

        // Step 3: Also scan entire ~/.m2/repository for jars not in pom.xml
        // This catches transitive dependencies and manually added jars
        scanAllMavenJars();

        return new LinkedHashSet<>(resolvedJars.stream().map(Paths::get).collect(java.util.stream.Collectors.toList()));
    }

    /**
     * Parse all relevant POM files in the project.
     * Handles multi-module projects and parent POMs.
     */
    private Map<String, String> parseAllPoms() throws IOException {
        Map<String, String> allDeps = new LinkedHashMap<>();

        // Find all pom.xml files
        List<Path> pomFiles = findPomFiles(projectRoot);

        for (Path pomPath : pomFiles) {
            try {
                PomInfo pom = parsePom(pomPath);

                // Add properties
                properties.putAll(pom.properties);

                // Add dependency management versions
                for (Map.Entry<String, String> entry : pom.dependencyManagement.entrySet()) {
                    dependencyVersions.putIfAbsent(entry.getKey(), entry.getValue());
                }

                // Add actual dependencies
                for (Map.Entry<String, String> entry : pom.dependencies.entrySet()) {
                    String gav = entry.getKey();
                    String version = resolveVersion(entry.getValue());
                    allDeps.put(gav, version);
                }
            } catch (Exception e) {
                // ignore parse errors
            }
        }

        return allDeps;
    }

    /**
     * Find all pom.xml files in the project.
     */
    private List<Path> findPomFiles(Path root) throws IOException {
        List<Path> poms = new ArrayList<>();

        // Look for pom.xml files in src and root
        Files.walk(root, 4) // limit depth
                .filter(p -> p.getFileName().toString().equals("pom.xml"))
                .filter(p -> !p.toString().contains("target"))
                .filter(p -> !p.toString().contains(".git"))
                .forEach(poms::add);

        // Sort: root pom.xml first, then sub-modules
        poms.sort((a, b) -> {
            String aPath = a.toString();
            String bPath = b.toString();
            // Root pom should come first
            if (aPath.equals(root.resolve("pom.xml").toString())) return -1;
            if (bPath.equals(root.resolve("pom.xml").toString())) return 1;
            return aPath.compareTo(bPath);
        });

        return poms;
    }

    /**
     * Parse a single POM file.
     */
    private PomInfo parsePom(Path pomPath) throws IOException {
        PomInfo pom = new PomInfo();

        String content = new String(Files.readAllBytes(pomPath));

        // Parse properties
        parseProperties(content, pom);

        // Parse parent POM (for dependency management)
        parseParent(content, pomPath, pom);

        // Parse dependency management
        parseDependencyManagement(content, pom);

        // Parse actual dependencies
        parseDependencies(content, pom);

        return pom;
    }

    private void parseProperties(String content, PomInfo pom) {
        Pattern propPattern = Pattern.compile("<properties>(.*?)</properties>", Pattern.DOTALL);
        Matcher matcher = propPattern.matcher(content);
        if (matcher.find()) {
            String propsSection = matcher.group(1);
            Pattern entryPattern = Pattern.compile("<([^>]+)>([^<]+)</\\1>");
            Matcher entryMatcher = entryPattern.matcher(propsSection);
            while (entryMatcher.find()) {
                pom.properties.put(entryMatcher.group(1), entryMatcher.group(2));
            }
        }
    }

    private void parseParent(String content, Path pomPath, PomInfo pom) {
        Pattern parentPattern = Pattern.compile("<parent>(.*?)</parent>", Pattern.DOTALL);
        Matcher matcher = parentPattern.matcher(content);
        if (matcher.find()) {
            String parentSection = matcher.group(1);
            String groupId = extractTag(parentSection, "groupId");
            String artifactId = extractTag(parentSection, "artifactId");
            String version = extractTag(parentSection, "version");

            if (groupId != null && artifactId != null && version != null) {
                // Try to find parent POM in ~/.m2/repository
                Path parentPom = m2Repo
                        .resolve(groupId.replace('.', '/'))
                        .resolve(artifactId)
                        .resolve(version)
                        .resolve(artifactId + "-" + version + ".pom");

                if (Files.exists(parentPom)) {
                    try {
                        String parentContent = new String(Files.readAllBytes(parentPom));
                        parseDependencyManagement(parentContent, pom);
                        parseProperties(parentContent, pom);

                        // Recursively check parent's parent
                        Pattern grandParentPattern = Pattern.compile("<parent>(.*?)</parent>", Pattern.DOTALL);
                        Matcher grandParentMatcher = grandParentPattern.matcher(parentContent);
                        if (grandParentMatcher.find()) {
                            String grandParentSection = grandParentMatcher.group(1);
                            String gpGroupId = extractTag(grandParentSection, "groupId");
                            String gpArtifactId = extractTag(grandParentSection, "artifactId");
                            String gpVersion = extractTag(grandParentSection, "version");

                            if (gpGroupId != null && gpArtifactId != null && gpVersion != null) {
                                Path grandParentPom = m2Repo
                                        .resolve(gpGroupId.replace('.', '/'))
                                        .resolve(gpArtifactId)
                                        .resolve(gpVersion)
                                        .resolve(gpArtifactId + "-" + gpVersion + ".pom");

                                if (Files.exists(grandParentPom)) {
                                    String grandParentContent = new String(Files.readAllBytes(grandParentPom));
                                    parseDependencyManagement(grandParentContent, pom);
                                    parseProperties(grandParentContent, pom);
                                }
                            }
                        }
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        }
    }

    private void parseDependencyManagement(String content, PomInfo pom) {
        // Find all <dependencyManagement><dependencies><dependency> blocks
        Pattern dmPattern = Pattern.compile("<dependencyManagement>(.*?)</dependencyManagement>", Pattern.DOTALL);
        Matcher dmMatcher = dmPattern.matcher(content);

        while (dmMatcher.find()) {
            String dmSection = dmMatcher.group(1);
            Pattern depPattern = Pattern.compile("<dependency>(.*?)</dependency>", Pattern.DOTALL);
            Matcher depMatcher = depPattern.matcher(dmSection);

            while (depMatcher.find()) {
                String depSection = depMatcher.group(1);
                String groupId = extractTag(depSection, "groupId");
                String artifactId = extractTag(depSection, "artifactId");
                String version = extractTag(depSection, "version");

                if (groupId != null && artifactId != null && version != null) {
                    String key = groupId + ":" + artifactId;
                    pom.dependencyManagement.put(key, resolveProperty(version, pom.properties));
                }
            }
        }
    }

    private void parseDependencies(String content, PomInfo pom) {
        Pattern depsPattern = Pattern.compile("<dependencies>(.*?)</dependencies>", Pattern.DOTALL);
        Matcher depsMatcher = depsPattern.matcher(content);

        while (depsMatcher.find()) {
            String depsSection = depsMatcher.group(1);
            Pattern depPattern = Pattern.compile("<dependency>(.*?)</dependency>", Pattern.DOTALL);
            Matcher depMatcher = depPattern.matcher(depsSection);

            while (depMatcher.find()) {
                String depSection = depMatcher.group(1);
                String groupId = extractTag(depSection, "groupId");
                String artifactId = extractTag(depSection, "artifactId");
                String version = extractTag(depSection, "version");
                String scope = extractTag(depSection, "scope");

                if (groupId != null && artifactId != null) {
                    // Skip test scope
                    if ("test".equals(scope)) continue;

                    String key = groupId + ":" + artifactId;
                    String resolvedVersion = version != null ? resolveProperty(version, pom.properties) : null;
                    pom.dependencies.put(key, resolvedVersion);
                }
            }
        }
    }

    private String extractTag(String content, String tagName) {
        Pattern pattern = Pattern.compile("<" + tagName + ">([^<]+)</" + tagName + ">");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private String resolveVersion(String version) {
        if (version == null) return null;
        return resolveProperty(version, properties);
    }

    private String resolveProperty(String value, Map<String, String> props) {
        if (value == null) return null;
        Pattern propRefPattern = Pattern.compile("\\$\\{([^}]+)\\}");
        Matcher matcher = propRefPattern.matcher(value);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String propName = matcher.group(1);
            String propValue = props.getOrDefault(propName, "");
            matcher.appendReplacement(sb, Matcher.quoteReplacement(propValue));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Resolve a single dependency to jar paths.
     */
    private void resolveJar(String groupId, String artifactId, String version) throws IOException {
        if (version == null || version.isEmpty()) return;

        String groupPath = groupId.replace('.', '/');
        Path depDir = m2Repo.resolve(groupPath).resolve(artifactId).resolve(version);

        if (Files.exists(depDir)) {
            Files.walk(depDir)
                    .filter(p -> p.toString().endsWith(".jar"))
                    .filter(p -> !p.toString().endsWith("-sources.jar"))
                    .filter(p -> !p.toString().endsWith("-javadoc.jar"))
                    .forEach(p -> resolvedJars.add(p.toString()));
        }
    }

    /**
     * Scan entire ~/.m2/repository for all jars (catches transitive deps).
     * Only scans commonly used paths to avoid excessive scanning.
     */
    private void scanAllMavenJars() throws IOException {
        if (!Files.exists(m2Repo)) return;

        // Only scan top-level group directories to limit scope
        Files.list(m2Repo)
                .filter(Files::isDirectory)
                .filter(dir -> {
                    // Skip non-maven directories
                    String name = dir.getFileName().toString();
                    return !name.startsWith(".") && !name.equals("archetype-catalog.xml");
                })
                .forEach(groupDir -> {
                    try {
                        Files.walk(groupDir, 3) // limit depth: group/artifact/version
                                .filter(p -> p.toString().endsWith(".jar"))
                                .filter(p -> !p.toString().contains("-sources"))
                                .filter(p -> !p.toString().contains("-javadoc"))
                                .filter(p -> !p.toString().contains("/test-classes/"))
                                .forEach(p -> resolvedJars.add(p.toString()));
                    } catch (IOException e) {
                        // ignore
                    }
                });
    }

    /**
     * Get jar count for logging.
     */
    public int getJarCount() {
        return resolvedJars.size();
    }

    static class PomInfo {
        Map<String, String> properties = new HashMap<>();
        Map<String, String> dependencyManagement = new HashMap<>();
        Map<String, String> dependencies = new LinkedHashMap<>();
    }
}
