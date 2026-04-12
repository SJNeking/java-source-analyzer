package cn.dolphinmind.glossary.java.analyze.monitoring;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Health Checker
 * 
 * Verifies the connectivity and status of all external dependencies.
 * Used for Docker/Kubernetes liveness and readiness probes.
 */
public class HealthChecker {

    private static final Logger logger = Logger.getLogger(HealthChecker.class.getName());

    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    private final String ollamaEndpoint;
    private final String minioEndpoint;

    public HealthChecker(String dbUrl, String dbUser, String dbPassword,
                         String ollamaEndpoint, String minioEndpoint) {
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.ollamaEndpoint = ollamaEndpoint;
        this.minioEndpoint = minioEndpoint;
    }

    /**
     * Run all health checks and return a summary.
     * @return Map of service name -> status (UP/DOWN) + details.
     */
    public Map<String, Object> checkAll() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("timestamp", System.currentTimeMillis());
        status.put("status", "UP"); // Default to UP

        Map<String, Object> components = new LinkedHashMap<>();
        
        // Check Database
        components.put("database", checkDatabase());
        
        // Check Ollama
        components.put("ollama", checkOllama());
        
        // Check MinIO
        components.put("minio", checkMinIO());

        // Overall status: if any component is DOWN, overall is DOWN
        for (Object comp : components.values()) {
            if (comp instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) comp;
                if ("DOWN".equals(map.get("status"))) {
                    status.put("status", "DOWN");
                    break;
                }
            }
        }

        status.put("components", components);
        return status;
    }

    private Map<String, Object> checkDatabase() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            if (dbUrl == null || dbUrl.isEmpty()) {
                result.put("status", "UNKNOWN");
                result.put("message", "Database not configured");
                return result;
            }
            
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
                // Simple query to verify connection
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT 1")) {
                    if (rs.next()) {
                        result.put("status", "UP");
                        result.put("version", conn.getMetaData().getDatabaseProductVersion());
                    }
                }
            }
        } catch (SQLException e) {
            result.put("status", "DOWN");
            result.put("error", e.getMessage());
        }
        return result;
    }

    private Map<String, Object> checkOllama() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            if (ollamaEndpoint == null || ollamaEndpoint.isEmpty()) {
                result.put("status", "UNKNOWN");
                result.put("message", "Ollama not configured");
                return result;
            }
            
            URL url = new URL(ollamaEndpoint + "/api/tags");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            int status = conn.getResponseCode();
            conn.disconnect();
            
            if (status == 200) {
                result.put("status", "UP");
            } else {
                result.put("status", "DOWN");
                result.put("code", status);
            }
        } catch (IOException e) {
            result.put("status", "DOWN");
            result.put("error", e.getMessage());
        }
        return result;
    }

    private Map<String, Object> checkMinIO() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            if (minioEndpoint == null || minioEndpoint.isEmpty()) {
                result.put("status", "UNKNOWN");
                result.put("message", "MinIO not configured");
                return result;
            }
            
            URL url = new URL(minioEndpoint + "/minio/health/live");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            int status = conn.getResponseCode();
            conn.disconnect();
            
            // MinIO returns 200 for live health check
            if (status == 200) {
                result.put("status", "UP");
            } else {
                result.put("status", "DOWN");
                result.put("code", status);
            }
        } catch (IOException e) {
            result.put("status", "DOWN");
            result.put("error", e.getMessage());
        }
        return result;
    }
}
