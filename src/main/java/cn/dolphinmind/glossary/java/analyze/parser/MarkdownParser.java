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
 * Parser for Markdown documentation files (README.md, docs/*.md)
 */
public class MarkdownParser implements FileParser {

    @Override
    public boolean supports(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        return name.endsWith(".md");
    }

    @Override
    public List<FileAsset> parse(Path file, String projectRoot) {
        List<FileAsset> assets = new ArrayList<>();
        try {
            String content = String.join("\n", Files.readAllLines(file, StandardCharsets.UTF_8));
            String fileName = file.getFileName().toString().toLowerCase();

            FileAsset asset = new FileAsset(
                    relativize(file, projectRoot).toString(),
                    FileAsset.AssetType.MARKDOWN_DOC
            );

            // Extract document structure
            List<String> headings = extractHeadings(content);
            asset.putMeta("headings", headings);
            asset.putMeta("heading_count", headings.size());

            // Extract code blocks
            int codeBlockCount = countCodeBlocks(content);
            asset.putMeta("code_block_count", codeBlockCount);

            // Extract links
            List<String> links = extractLinks(content);
            asset.putMeta("links", links);
            asset.putMeta("link_count", links.size());

            // Extract key sections (for README)
            if (fileName.equals("readme.md")) {
                Map<String, String> sections = extractReadmeSections(content);
                asset.putMeta("sections", sections);

                // Detect project description
                String description = extractDescription(content);
                asset.putMeta("description", description);

                // Detect build instructions
                boolean hasBuildInstructions = content.toLowerCase().contains("build") ||
                        content.toLowerCase().contains("编译");
                asset.putMeta("has_build_instructions", hasBuildInstructions);

                // Detect quick start
                boolean hasQuickStart = content.toLowerCase().contains("quick start") ||
                        content.toLowerCase().contains("快速开始");
                asset.putMeta("has_quick_start", hasQuickStart);
            }

            // Content stats
            asset.putMeta("line_count", content.split("\n").length);
            asset.putMeta("word_count", content.split("\\s+").length);
            asset.putMeta("content_preview", content.length() > 500 ? content.substring(0, 500) + "..." : content);

            assets.add(asset);
        } catch (Exception e) {
            FileAsset asset = new FileAsset(
                    relativize(file, projectRoot).toString(),
                    FileAsset.AssetType.MARKDOWN_DOC
            );
            asset.putMeta("error", e.getMessage());
            assets.add(asset);
        }
        return assets;
    }

    @Override
    public FileAsset.AssetType getAssetType() {
        return FileAsset.AssetType.MARKDOWN_DOC;
    }

    private List<String> extractHeadings(String content) {
        List<String> headings = new ArrayList<>();
        Pattern pattern = Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            String level = matcher.group(1);
            String text = matcher.group(2).trim();
            headings.add("H" + level.length() + ": " + text);
        }
        return headings;
    }

    private int countCodeBlocks(String content) {
        int count = 0;
        Pattern pattern = Pattern.compile("```");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) count++;
        return count / 2; // Each code block has opening and closing ```
    }

    private List<String> extractLinks(String content) {
        List<String> links = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\[([^\\]]+)\\]\\(([^)]+)\\)");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            links.add(matcher.group(1) + " -> " + matcher.group(2));
        }
        return links;
    }

    private Map<String, String> extractReadmeSections(String content) {
        Map<String, String> sections = new LinkedHashMap<>();
        Pattern pattern = Pattern.compile("^##\\s+(.+)$", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(content);

        int lastEnd = 0;
        String lastHeading = "";

        while (matcher.find()) {
            if (!lastHeading.isEmpty()) {
                String sectionContent = content.substring(lastEnd, matcher.start()).trim();
                sections.put(lastHeading, sectionContent.length() > 300 ? sectionContent.substring(0, 300) + "..." : sectionContent);
            }
            lastHeading = matcher.group(1).trim();
            lastEnd = matcher.end();
        }

        // Last section
        if (!lastHeading.isEmpty()) {
            String sectionContent = content.substring(lastEnd).trim();
            sections.put(lastHeading, sectionContent.length() > 300 ? sectionContent.substring(0, 300) + "..." : sectionContent);
        }

        return sections;
    }

    private String extractDescription(String content) {
        // Look for the first paragraph that is not a heading or code block
        String[] paragraphs = content.split("\n\n");
        for (String para : paragraphs) {
            String trimmed = para.trim();
            if (!trimmed.startsWith("#") && !trimmed.startsWith("```") && !trimmed.startsWith("[") && trimmed.length() > 20) {
                return trimmed.length() > 200 ? trimmed.substring(0, 200) + "..." : trimmed;
            }
        }
        return "";
    }
}
