package cn.dolphinmind.glossary.java.analyze.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Registry of all file parsers. Add new parsers here.
 */
public class ParserRegistry {

    private static final List<FileParser> PARSERS = Arrays.asList(
            new PomXmlParser(),
            new ConfigFileParser(),
            new SqlFileParser(),
            new MyBatisXmlParser(),
            new MarkdownParser(),
            new DockerfileParser(),
            new LogConfigParser(),
            new DockerComposeParser(),
            new ShellScriptParser()
    );

    /**
     * Find the first parser that supports the given file
     */
    public static FileParser findParser(Path file) {
        return PARSERS.stream()
                .filter(p -> p.supports(file))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get all registered parsers
     */
    public static List<FileParser> getAllParsers() {
        return PARSERS;
    }

    /**
     * Register a new parser at runtime
     */
    public static void registerParser(FileParser parser) {
        PARSERS.add(parser);
    }
}
