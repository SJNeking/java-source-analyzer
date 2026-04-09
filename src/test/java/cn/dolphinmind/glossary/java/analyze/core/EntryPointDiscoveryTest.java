package cn.dolphinmind.glossary.java.analyze.core;

import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests for EntryPointDiscovery.
 */
public class EntryPointDiscoveryTest {

    private final EntryPointDiscovery discovery = new EntryPointDiscovery();

    @Test
    public void discover_shouldFindMainMethod() throws Exception {
        // This test uses the actual project source
        Path projectRoot = Paths.get(System.getProperty("user.dir"));
        List<EntryPointDiscovery.EntryPoint> entries = discovery.discover(projectRoot);

        // Should find at least one main method
        boolean hasMain = entries.stream().anyMatch(e -> e.getType() == EntryPointDiscovery.EntryPointType.MAIN_METHOD);
        assertTrue("Should find main method", hasMain);
    }

    @Test
    public void discover_shouldNotFindTestClasses() throws Exception {
        Path projectRoot = Paths.get(System.getProperty("user.dir"));
        List<EntryPointDiscovery.EntryPoint> entries = discovery.discover(projectRoot);

        // Should not find entry points in test directories
        boolean inTest = entries.stream().anyMatch(e -> e.getFilePath().contains("test"));
        assertFalse("Should not find entry points in test directories", inTest);
    }

    @Test
    public void entryPoint_toMap_shouldReturnCompleteMap() {
        EntryPointDiscovery.EntryPoint ep = new EntryPointDiscovery.EntryPoint(
            "MyController",
            "com.example",
            "getUser",
            "src/main/java/com/example/MyController.java",
            EntryPointDiscovery.EntryPointType.REST_CONTROLLER,
            "/api/users/{id}",
            "GET",
            42
        );

        Map<String, Object> map = ep.toMap();
        assertEquals("com.example.MyController", map.get("class"));
        assertEquals("getUser", map.get("method"));
        assertEquals("REST_CONTROLLER", map.get("type"));
    }

    @Test
    public void groupByType_shouldGroupCorrectly() {
        List<EntryPointDiscovery.EntryPoint> entries = Arrays.asList(
            new EntryPointDiscovery.EntryPoint("Main", "", "main", "Main.java",
                EntryPointDiscovery.EntryPointType.MAIN_METHOD, "", "", 1),
            new EntryPointDiscovery.EntryPoint("Controller", "", "get", "Controller.java",
                EntryPointDiscovery.EntryPointType.REST_CONTROLLER, "/api", "GET", 2),
            new EntryPointDiscovery.EntryPoint("Controller", "", "post", "Controller.java",
                EntryPointDiscovery.EntryPointType.REST_CONTROLLER, "/api", "POST", 3)
        );

        Map<EntryPointDiscovery.EntryPointType, List<EntryPointDiscovery.EntryPoint>> grouped =
            discovery.groupByType(entries);

        assertEquals(1, grouped.get(EntryPointDiscovery.EntryPointType.MAIN_METHOD).size());
        assertEquals(2, grouped.get(EntryPointDiscovery.EntryPointType.REST_CONTROLLER).size());
    }
}
