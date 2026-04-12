package cn.dolphinmind.glossary.java.analyze.security;

import org.junit.Test;
import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Path;

public class SecurityUtilsTest {

    @Test
    public void testValidatePathSuccess() {
        // Should allow normal paths
        Path p = SecurityUtils.validatePath("src/main/java", new File("."));
        assertNotNull(p);
        System.out.println("[PASS] Path Validation: Allowed valid path");
    }

    @Test(expected = SecurityException.class)
    public void testValidatePathTraversal() {
        // Should block path traversal
        SecurityUtils.validatePath("../../etc/passwd", new File("."));
    }

    @Test
    public void testValidateExtensionSuccess() {
        // Should allow JSON
        SecurityUtils.validateExtension("data.json", "json");
        System.out.println("[PASS] Extension Validation: Allowed .json");
    }

    @Test(expected = SecurityException.class)
    public void testValidateExtensionFail() {
        // Should block EXE/BAT
        SecurityUtils.validateExtension("payload.exe", "json");
    }

    @Test
    public void testSanitizeLogInput() {
        String input = "User injected \r\n malicious line";
        String clean = SecurityUtils.sanitizeLogInput(input);
        assertFalse("Should not contain newlines", clean.contains("\n"));
        assertFalse("Should not contain carriage returns", clean.contains("\r"));
        System.out.println("[PASS] Log Sanitization: Removed CRLF");
    }
}