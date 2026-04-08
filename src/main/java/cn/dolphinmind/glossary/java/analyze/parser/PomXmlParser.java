package cn.dolphinmind.glossary.java.analyze.parser;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
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
 * Parser for Maven pom.xml files
 */
public class PomXmlParser implements FileParser {

    @Override
    public boolean supports(Path file) {
        String name = file.getFileName().toString();
        return name.equals("pom.xml");
    }

    @Override
    public List<FileAsset> parse(Path file, String projectRoot) {
        List<FileAsset> assets = new ArrayList<>();
        try {
            String content = String.join("\n", Files.readAllLines(file, StandardCharsets.UTF_8));
            FileAsset asset = new FileAsset(
                    relativize(file, projectRoot).toString(),
                    FileAsset.AssetType.MAVEN_POM
            );

            // Extract Maven coordinates
            asset.putMeta("groupId", extractXmlValue(content, "groupId"));
            asset.putMeta("artifactId", extractXmlValue(content, "artifactId"));
            asset.putMeta("version", extractXmlValue(content, "version"));
            asset.putMeta("packaging", extractXmlValue(content, "packaging"));
            asset.putMeta("name", extractXmlValue(content, "name"));
            asset.putMeta("description", extractXmlValue(content, "description"));

            // Extract parent info
            Map<String, String> parent = new LinkedHashMap<>();
            parent.put("groupId", extractXmlParentValue(content, "groupId"));
            parent.put("artifactId", extractXmlParentValue(content, "artifactId"));
            parent.put("version", extractXmlParentValue(content, "version"));
            asset.putMeta("parent", parent);

            // Extract properties
            asset.putMeta("properties", extractProperties(content));

            // Extract dependencies
            asset.putMeta("dependencies", extractDependencies(content, "<dependencies>", "</dependencies>"));

            // Extract dependencyManagement
            asset.putMeta("dependency_management", extractDependencies(content, "<dependencyManagement>", "</dependencyManagement>"));

            // Extract plugins
            asset.putMeta("plugins", extractPlugins(content));

            // Extract modules
            asset.putMeta("modules", extractModules(content));

            // Extract profiles
            asset.putMeta("profiles", extractProfiles(content));

            assets.add(asset);
        } catch (Exception e) {
            FileAsset asset = new FileAsset(
                    relativize(file, projectRoot).toString(),
                    FileAsset.AssetType.MAVEN_POM
            );
            asset.putMeta("error", e.getMessage());
            assets.add(asset);
        }
        return assets;
    }

    @Override
    public FileAsset.AssetType getAssetType() {
        return FileAsset.AssetType.MAVEN_POM;
    }

    private String extractXmlValue(String xml, String tagName) {
        // Use DOM parser for robust XML parsing
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));

            // Search at any level (root, children, etc.)
            NodeList nodes = doc.getElementsByTagName(tagName);
            for (int i = 0; i < nodes.getLength(); i++) {
                Element el = (Element) nodes.item(i);
                // Skip if inside <parent> block
                if (isInParentBlock(el)) continue;
                String text = el.getTextContent();
                if (text != null && !text.trim().isEmpty()) {
                    return text.trim();
                }
            }
        } catch (Exception e) {
            // Fallback to regex
            return extractXmlValueRegex(xml, tagName);
        }
        return "";
    }

    private boolean isInParentBlock(Element el) {
        Node parent = el.getParentNode();
        while (parent != null) {
            if ("parent".equals(parent.getNodeName())) return true;
            parent = parent.getParentNode();
        }
        return false;
    }

    private String extractXmlValueRegex(String xml, String tagName) {
        Pattern pattern = Pattern.compile("^\\s*<" + tagName + ">([^<]+)</" + tagName + ">", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(xml);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    private String extractXmlParentValue(String xml, String tagName) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));

            NodeList parents = doc.getElementsByTagName("parent");
            if (parents.getLength() > 0) {
                Element parent = (Element) parents.item(0);
                NodeList tags = parent.getElementsByTagName(tagName);
                if (tags.getLength() > 0) {
                    return tags.item(0).getTextContent().trim();
                }
            }
        } catch (Exception e) {
            // Fallback
        }
        return "";
    }

    private Map<String, String> extractProperties(String content) {
        Map<String, String> props = new LinkedHashMap<>();
        Pattern propPattern = Pattern.compile("<properties>(.*?)</properties>", Pattern.DOTALL);
        Matcher propMatcher = propPattern.matcher(content);
        if (propMatcher.find()) {
            String propsBlock = propMatcher.group(1);
            Pattern entryPattern = Pattern.compile("<(\\w[^>]+)>([^<]+)</\\1>");
            Matcher entryMatcher = entryPattern.matcher(propsBlock);
            while (entryMatcher.find()) {
                props.put(entryMatcher.group(1), entryMatcher.group(2).trim());
            }
        }
        return props;
    }

    private List<Map<String, String>> extractDependencies(String content, String startTag, String endTag) {
        List<Map<String, String>> deps = new ArrayList<>();
        int startIdx = content.indexOf(startTag);
        int endIdx = content.indexOf(endTag);
        if (startIdx == -1 || endIdx == -1 || endIdx <= startIdx) return deps;

        String depBlock = content.substring(startIdx, endIdx);
        Pattern depPattern = Pattern.compile(
                "<dependency>\\s*<groupId>([^<]+)</groupId>\\s*<artifactId>([^<]+)</artifactId>(?:\\s*<version>([^<]+)</version>)?(?:\\s*<scope>([^<]+)</scope>)?",
                Pattern.DOTALL);
        Matcher matcher = depPattern.matcher(depBlock);
        while (matcher.find()) {
            Map<String, String> dep = new LinkedHashMap<>();
            dep.put("groupId", matcher.group(1).trim());
            dep.put("artifactId", matcher.group(2).trim());
            dep.put("version", matcher.group(3) != null ? matcher.group(3).trim() : "");
            dep.put("scope", matcher.group(4) != null ? matcher.group(4).trim() : "compile");
            deps.add(dep);
        }
        return deps;
    }

    private List<Map<String, String>> extractPlugins(String content) {
        List<Map<String, String>> plugins = new ArrayList<>();
        Pattern pluginPattern = Pattern.compile(
                "<plugin>\\s*<groupId>([^<]+)</groupId>\\s*<artifactId>([^<]+)</artifactId>(?:\\s*<version>([^<]+)</version>)?",
                Pattern.DOTALL);
        Matcher matcher = pluginPattern.matcher(content);
        while (matcher.find()) {
            Map<String, String> plugin = new LinkedHashMap<>();
            plugin.put("groupId", matcher.group(1).trim());
            plugin.put("artifactId", matcher.group(2).trim());
            plugin.put("version", matcher.group(3) != null ? matcher.group(3).trim() : "");
            plugins.add(plugin);
        }
        return plugins;
    }

    private List<String> extractModules(String content) {
        List<String> modules = new ArrayList<>();
        Pattern modulePattern = Pattern.compile("<module>([^<]+)</module>");
        Matcher matcher = modulePattern.matcher(content);
        while (matcher.find()) {
            modules.add(matcher.group(1).trim());
        }
        return modules;
    }

    private List<Map<String, String>> extractProfiles(String content) {
        List<Map<String, String>> profiles = new ArrayList<>();
        Pattern profilePattern = Pattern.compile("<profile>\\s*<id>([^<]+)</id>", Pattern.DOTALL);
        Matcher matcher = profilePattern.matcher(content);
        while (matcher.find()) {
            Map<String, String> profile = new LinkedHashMap<>();
            profile.put("id", matcher.group(1).trim());
            profiles.add(profile);
        }
        return profiles;
    }
}
