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
 * Parser for logback.xml / log4j2.xml logging configuration files
 */
public class LogConfigParser implements FileParser {

    @Override
    public boolean supports(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        return name.equals("logback.xml") || name.equals("log4j2.xml") ||
               name.equals("logback-spring.xml") || name.equals("log4j2-spring.xml");
    }

    @Override
    public List<FileAsset> parse(Path file, String projectRoot) {
        List<FileAsset> assets = new ArrayList<>();
        try {
            String content = String.join("\n", Files.readAllLines(file, StandardCharsets.UTF_8));
            String fileName = file.getFileName().toString().toLowerCase();
            boolean isLogback = fileName.contains("logback");

            FileAsset asset = new FileAsset(
                    relativize(file, projectRoot),
                    FileAsset.AssetType.LOG_CONFIG
            );

            if (isLogback) {
                parseLogback(content, asset);
            } else {
                parseLog4j2(content, asset);
            }

            asset.putMeta("line_count", content.split("\n").length);
            assets.add(asset);
        } catch (Exception e) {
            FileAsset asset = new FileAsset(
                    relativize(file, projectRoot),
                    FileAsset.AssetType.LOG_CONFIG
            );
            asset.putMeta("error", e.getMessage());
            assets.add(asset);
        }
        return assets;
    }

    @Override
    public FileAsset.AssetType getAssetType() {
        return FileAsset.AssetType.LOG_CONFIG;
    }

    private void parseLogback(String content, FileAsset asset) {
        // Extract root level
        String rootLevel = extractXmlAttribute(content, "level", "root");
        asset.putMeta("root_level", rootLevel);

        // Extract logger definitions
        List<Map<String, String>> loggers = new ArrayList<>();
        Pattern loggerPattern = Pattern.compile(
                "<logger\\s+name=\"([^\"]+)\"(?:\\s+level=\"([^\"]+)\")?");
        Matcher loggerMatcher = loggerPattern.matcher(content);
        while (loggerMatcher.find()) {
            Map<String, String> logger = new LinkedHashMap<>();
            logger.put("name", loggerMatcher.group(1));
            logger.put("level", loggerMatcher.group(2) != null ? loggerMatcher.group(2) : "inherit");
            loggers.add(logger);
        }
        asset.putMeta("loggers", loggers);
        asset.putMeta("logger_count", loggers.size());

        // Extract appenders
        List<Map<String, String>> appenders = new ArrayList<>();
        Pattern appenderPattern = Pattern.compile(
                "<appender\\s+name=\"([^\"]+)\"\\s+class=\"([^\"]+)\"");
        Matcher appenderMatcher = appenderPattern.matcher(content);
        while (appenderMatcher.find()) {
            Map<String, String> appender = new LinkedHashMap<>();
            appender.put("name", appenderMatcher.group(1));
            appender.put("class", appenderMatcher.group(2));

            // Detect type
            String className = appenderMatcher.group(2);
            if (className.contains("ConsoleAppender")) appender.put("type", "console");
            else if (className.contains("FileAppender") || className.contains("RollingFileAppender")) {
                appender.put("type", "file");
                // Extract file path
                Pattern filePattern = Pattern.compile("<file>([^<]+)</file>");
                Matcher fileMatcher = filePattern.matcher(content);
                if (fileMatcher.find()) appender.put("file_path", fileMatcher.group(1));
            }
            else appender.put("type", "other");

            appenders.add(appender);
        }
        asset.putMeta("appenders", appenders);
        asset.putMeta("appender_count", appenders.size());
    }

    private void parseLog4j2(String content, FileAsset asset) {
        // Extract root logger level
        String rootLevel = extractXmlTagValue(content, "Root", "level");
        asset.putMeta("root_level", rootLevel);

        // Extract appenders
        List<Map<String, String>> appenders = new ArrayList<>();
        Pattern appenderPattern = Pattern.compile(
                "<([^>]+)\\s+name=\"([^\"]+)\"");
        Matcher appenderMatcher = appenderPattern.matcher(content);
        while (appenderMatcher.find()) {
            Map<String, String> appender = new LinkedHashMap<>();
            appender.put("type", appenderMatcher.group(1));
            appender.put("name", appenderMatcher.group(2));
            appenders.add(appender);
        }
        asset.putMeta("appenders", appenders);
    }

    private String extractXmlAttribute(String xml, String attrName, String tag) {
        Pattern pattern = Pattern.compile("<" + tag + "[^>]+" + attrName + "\\s*=\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(xml);
        if (matcher.find()) return matcher.group(1);
        return "";
    }

    private String extractXmlTagValue(String xml, String tag, String attrName) {
        Pattern pattern = Pattern.compile("<" + tag + "[^>]+" + attrName + "\\s*=\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(xml);
        if (matcher.find()) return matcher.group(1);
        return "";
    }
}
