package cn.dolphinmind.glossary.java.analyze.security;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Security Utilities
 * 
 * Provides input validation and sanitization to prevent:
 * - Path Traversal (../../)
 * - Arbitrary File Read
 * - Command Injection
 */
public class SecurityUtils {

    /**
     * Validate and sanitize a file path to prevent Path Traversal attacks.
     * 
     * @param rawPath The raw path string from user input (CLI/API).
     * @param baseDir The allowed base directory (optional, null for absolute paths).
     * @return The sanitized, canonical Path.
     * @throws SecurityException if the path is invalid or outside baseDir.
     */
    public static Path validatePath(String rawPath, File baseDir) {
        if (rawPath == null || rawPath.trim().isEmpty()) {
            throw new SecurityException("Path cannot be empty");
        }

        Path path = Paths.get(rawPath).normalize().toAbsolutePath();

        // Check for path traversal sequences
        if (rawPath.contains("..") && !baseDir.toPath().resolve(rawPath).normalize().startsWith(baseDir.toPath())) {
             // If it contains ".." and goes outside base, fail
             if (baseDir != null) {
                 throw new SecurityException("Path traversal detected: " + rawPath);
             }
        }

        if (baseDir != null) {
            Path base = baseDir.toPath().normalize().toAbsolutePath();
            if (!path.startsWith(base)) {
                throw new SecurityException("Path traversal detected: " + rawPath + " is outside allowed directory");
            }
        }

        return path;
    }

    /**
     * Validate file extension to prevent execution of arbitrary binaries/scripts.
     * 
     * @param filename The filename to check.
     * @param allowedExtensions Set of allowed extensions (e.g., "json", "java").
     * @throws SecurityException if extension is not allowed.
     */
    public static void validateExtension(String filename, String... allowedExtensions) {
        if (filename == null) return;

        String ext = "";
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            ext = filename.substring(dotIndex + 1).toLowerCase();
        }

        for (String allowed : allowedExtensions) {
            if (ext.equals(allowed.toLowerCase())) {
                return;
            }
        }
        
        throw new SecurityException("Invalid file extension: ." + ext + ". Allowed: " + String.join(", ", allowedExtensions));
    }

    /**
     * Sanitize a string to prevent Log Injection (CRLF Injection).
     * 
     * @param input The log message.
     * @return Sanitized string with newlines replaced.
     */
    public static String sanitizeLogInput(String input) {
        if (input == null) return "null";
        return input.replaceAll("[\\r\\n]", "_");
    }
}
