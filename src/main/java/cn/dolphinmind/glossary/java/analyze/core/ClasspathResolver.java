package cn.dolphinmind.glossary.java.analyze.core;

import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserSymbolDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * Builds the classpath for JavaParser Symbol Solver.
 *
 * Sources:
 * 1. Compiled classes (target/classes, target/test-classes)
 * 2. Maven dependencies (~/.m2/repository)
 * 3. Java runtime (rt.jar or jmods)
 */
public class ClasspathResolver {

    /**
     * Create a CombinedTypeSolver with all available type sources.
     */
    public static CombinedTypeSolver create(Path projectRoot) throws IOException {
        CombinedTypeSolver solver = new CombinedTypeSolver();

        // 1. Java runtime types
        solver.add(new ReflectionTypeSolver());

        // 2. Project source files
        Path srcDir = projectRoot.resolve("src/main/java");
        if (Files.exists(srcDir)) {
            solver.add(new com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver(srcDir));
        }

        // 3. Compiled classes from target directory
        Path targetClasses = projectRoot.resolve("target/classes");
        if (Files.exists(targetClasses)) {
            solver.add(new com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver(targetClasses));
        }

        // 4. Maven dependencies
        List<Path> jars = resolveMavenDependencies(projectRoot);
        for (Path jar : jars) {
            try {
                solver.add(new JarTypeSolver(jar));
            } catch (Exception e) {
                // ignore jars that can't be read
            }
        }

        return solver;
    }

    /**
     * Resolve Maven dependencies from the project's pom.xml.
     * Searches ~/.m2/repository for groupId/artifactId/version/*.jar
     */
    private static List<Path> resolveMavenDependencies(Path projectRoot) throws IOException {
        List<Path> jars = new ArrayList<>();
        Path pomFile = projectRoot.resolve("pom.xml");
        if (!Files.exists(pomFile)) return jars;

        String m2Repo = System.getProperty("user.home") + "/.m2/repository";
        Path m2Path = Paths.get(m2Repo);
        if (!Files.exists(m2Path)) return jars;

        // Parse pom.xml to get dependencies
        List<String[]> dependencies = parsePomDependencies(pomFile);

        for (String[] dep : dependencies) {
            String groupId = dep[0];
            String artifactId = dep[1];
            String version = dep[2];

            // Convert groupId to path: org.springframework -> org/springframework
            String groupPath = groupId.replace('.', '/');
            Path depDir = m2Path.resolve(groupPath).resolve(artifactId).resolve(version);

            if (Files.exists(depDir)) {
                try {
                    Files.walk(depDir)
                            .filter(p -> p.toString().endsWith(".jar"))
                            .filter(p -> !p.toString().endsWith("-sources.jar"))
                            .filter(p -> !p.toString().endsWith("-javadoc.jar"))
                            .forEach(jars::add);
                } catch (IOException e) {
                    // ignore
                }
            }
        }

        return jars;
    }

    /**
     * Simple pom.xml parser to extract dependencies.
     * Extracts groupId, artifactId, version from <dependency> blocks.
     */
    private static List<String[]> parsePomDependencies(Path pomFile) throws IOException {
        List<String[]> deps = new ArrayList<>();
        try {
            String content = new String(Files.readAllBytes(pomFile));
            String[] lines = content.split("\n");

            boolean inDependency = false;
            String groupId = null, artifactId = null, version = null;

            for (String line : lines) {
                String trimmed = line.trim();

                if (trimmed.contains("<dependency>") && !trimmed.contains("</dependency>")) {
                    inDependency = true;
                    groupId = null; artifactId = null; version = null;
                    continue;
                }

                if (trimmed.contains("</dependency>")) {
                    if (groupId != null && artifactId != null && version != null) {
                        deps.add(new String[]{groupId, artifactId, version});
                    }
                    inDependency = false;
                    continue;
                }

                if (!inDependency) continue;

                if (trimmed.startsWith("<groupId>") && groupId == null) {
                    groupId = extractXmlValue(trimmed);
                } else if (trimmed.startsWith("<artifactId>") && artifactId == null) {
                    artifactId = extractXmlValue(trimmed);
                } else if (trimmed.startsWith("<version>") && version == null) {
                    version = extractXmlValue(trimmed);
                }
            }
        } catch (Exception e) {
            // ignore parse errors
        }

        return deps;
    }

    private static String extractXmlValue(String line) {
        int start = line.indexOf('>');
        int end = line.indexOf('<', start + 1);
        if (start >= 0 && end > start) {
            return line.substring(start + 1, end).trim();
        }
        return null;
    }
}
