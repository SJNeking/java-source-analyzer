package cn.dolphinmind.glossary.java.analyze.parser;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Base interface for all file parsers in the project
 */
public interface FileParser {

    /**
     * Check if this parser can handle the given file
     */
    boolean supports(Path file);

    /**
     * Parse the file and return a list of assets
     */
    List<FileAsset> parse(Path file, String projectRoot);

    /**
     * Get the asset types this parser produces
     */
    FileAsset.AssetType getAssetType();

    /**
     * Helper: relativize a path against project root
     */
    default String relativize(Path file, String projectRoot) {
        Path root = Paths.get(projectRoot);
        return root.relativize(file).toString();
    }
}
