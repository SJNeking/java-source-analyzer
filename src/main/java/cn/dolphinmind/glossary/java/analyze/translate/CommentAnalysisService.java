package cn.dolphinmind.glossary.java.analyze.translate;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.JavadocComment;

import java.util.*;
import java.util.regex.*;

/**
 * Extracts and parses comments/Javadoc from Java AST nodes.
 *
 * Extracted from SourceUniversePro to decouple comment analysis from the main pipeline.
 * Handles:
 * - Comment detection (line, block, javadoc)
 * - Javadoc tag extraction (@param, @return, @throws, @see, @since, @deprecated)
 * - Semantic note extraction (TODO, FIXME, NOTE, HACK, XXX)
 */
public class CommentAnalysisService {

    /**
     * Extract comment details from a node including Javadoc tags and semantic notes.
     */
    public Map<String, Object> extractCommentDetails(List<String> lines, Node node) {
        Map<String, Object> result = new LinkedHashMap<>();
        Optional<Comment> commentOpt = node.getComment();

        if (commentOpt.isPresent()) {
            Comment comment = commentOpt.get();
            String raw = comment.getContent();
            String style = comment instanceof JavadocComment ? "javadoc" :
                           raw.trim().startsWith("*") ? "block" : "line";

            result.put("raw", raw);
            result.put("style", style);
            result.put("line", comment.getBegin().map(p -> p.line).orElse(0));

            // Clean comment: remove */ /* and leading *
            String cleaned = cleanCommentContent(raw);
            result.put("cleaned", cleaned);

            // Extract summary (first sentence or first line)
            String summary = extractSummary(cleaned);
            result.put("summary", summary);

            // Extract Javadoc tags
            if (style.equals("javadoc")) {
                extractJavadocTags(raw, result);
            }

            // Extract semantic notes
            extractSemanticNotes(cleaned, result);

        } else {
            result.put("summary", "");
            result.put("style", "none");
        }

        return result;
    }

    /**
     * Quick brute-force comment extraction for a node (returns just the text).
     */
    public String bruteForceComment(List<String> lines, Node node) {
        Optional<Comment> commentOpt = node.getComment();
        if (commentOpt.isPresent()) {
            String raw = commentOpt.get().getContent();
            // Return first non-empty line as summary
            String[] parts = raw.split("\\n");
            for (String part : parts) {
                String trimmed = part.trim().replaceAll("^[/\\*\\s]+", "").replaceAll("[\\*/]+$", "").trim();
                if (!trimmed.isEmpty()) return trimmed;
            }
        }
        return "";
    }

    /**
     * Count comment lines for a type declaration.
     */
    public int countCommentLines(List<String> fileLines, com.github.javaparser.ast.body.TypeDeclaration<?> type) {
        int count = 0;
        Optional<com.github.javaparser.ast.comments.Comment> commentOpt = type.getComment();
        if (commentOpt.isPresent()) {
            String content = commentOpt.get().getContent();
            count += content.split("\\n").length;
        }
        return count;
    }

    // =====================================================================
    // Internal helpers
    // =====================================================================

    private String cleanCommentContent(String raw) {
        if (raw == null) return "";
        // Remove */ and /*
        String cleaned = raw.replaceAll("\\*/", "").replaceAll("/\\*", "");
        // Remove leading * on each line
        String[] lines = cleaned.split("\\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim().replaceAll("^\\*+\\s?", "");
            sb.append(trimmed).append("\n");
        }
        return sb.toString().trim();
    }

    private String extractSummary(String cleaned) {
        if (cleaned == null || cleaned.isEmpty()) return "";
        // First sentence (up to . followed by space or newline)
        int dotIndex = cleaned.indexOf(". ");
        if (dotIndex > 0) return cleaned.substring(0, dotIndex + 1).trim();
        // Or first line
        int newlineIndex = cleaned.indexOf("\n");
        if (newlineIndex > 0) return cleaned.substring(0, newlineIndex).trim();
        return cleaned.length() > 200 ? cleaned.substring(0, 200) + "..." : cleaned;
    }

    /**
     * Extract Javadoc tags: @param, @return, @throws, @see, @since, @deprecated
     */
    public void extractJavadocTags(String rawComment, Map<String, Object> result) {
        Map<String, Object> tags = new LinkedHashMap<>();

        // @param tags
        List<Map<String, String>> params = new ArrayList<>();
        Matcher paramMatcher = Pattern.compile("@param\\s+(\\w+)\\s+(.+?)(?=@|$)", Pattern.DOTALL).matcher(rawComment);
        while (paramMatcher.find()) {
            Map<String, String> p = new LinkedHashMap<>();
            p.put("name", paramMatcher.group(1));
            p.put("description", paramMatcher.group(2).trim());
            params.add(p);
        }
        if (!params.isEmpty()) tags.put("params", params);

        // @return
        Matcher returnMatcher = Pattern.compile("@return\\s+(.+?)(?=@|$)", Pattern.DOTALL).matcher(rawComment);
        if (returnMatcher.find()) {
            tags.put("return", returnMatcher.group(1).trim());
        }

        // @throws / @exception
        List<Map<String, String>> throwsList = new ArrayList<>();
        Matcher throwsMatcher = Pattern.compile("@(?:throws|exception)\\s+(\\S+)\\s+(.+?)(?=@|$)", Pattern.DOTALL).matcher(rawComment);
        while (throwsMatcher.find()) {
            Map<String, String> t = new LinkedHashMap<>();
            t.put("exception", throwsMatcher.group(1));
            t.put("description", throwsMatcher.group(2).trim());
            throwsList.add(t);
        }
        if (!throwsList.isEmpty()) tags.put("throws", throwsList);

        // @see
        List<String> sees = new ArrayList<>();
        Matcher seeMatcher = Pattern.compile("@see\\s+(.+)", Pattern.MULTILINE).matcher(rawComment);
        while (seeMatcher.find()) {
            sees.add(seeMatcher.group(1).trim());
        }
        if (!sees.isEmpty()) tags.put("see", sees);

        // @since
        Matcher sinceMatcher = Pattern.compile("@since\\s+(.+)", Pattern.MULTILINE).matcher(rawComment);
        if (sinceMatcher.find()) {
            tags.put("since", sinceMatcher.group(1).trim());
        }

        // @deprecated
        Matcher deprecatedMatcher = Pattern.compile("@deprecated\\s*(.*)", Pattern.MULTILINE).matcher(rawComment);
        if (deprecatedMatcher.find()) {
            tags.put("deprecated", deprecatedMatcher.group(1).trim());
        }

        if (!tags.isEmpty()) {
            result.put("javadoc_tags", tags);
        }
    }

    /**
     * Extract semantic notes: TODO, FIXME, NOTE, HACK, XXX
     */
    public void extractSemanticNotes(String cleanedComment, Map<String, Object> result) {
        if (cleanedComment == null || cleanedComment.isEmpty()) return;

        List<String> notes = new ArrayList<>();
        for (String keyword : Arrays.asList("TODO", "FIXME", "NOTE", "HACK", "XXX", "WORKAROUND")) {
            Matcher m = Pattern.compile("\\b" + keyword + "\\b[:\\s]*(.+?)(?:\\n|$)", Pattern.CASE_INSENSITIVE).matcher(cleanedComment);
            while (m.find()) {
                notes.add(keyword + ": " + m.group(1).trim());
            }
        }

        if (!notes.isEmpty()) {
            Map<String, Object> semanticNotes = new LinkedHashMap<>();
            semanticNotes.put("notes", notes);
            result.put("semantic_notes", semanticNotes);
        }
    }
}
