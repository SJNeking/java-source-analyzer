package cn.dolphinmind.glossary.java.analyze.rag.store;

import cn.dolphinmind.glossary.java.analyze.rag.model.RagSlice;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * PGVector 向量存储实现
 * 
 * 使用 PostgreSQL + pgvector 扩展存储和检索向量
 * 
 * 前提:
 *   CREATE EXTENSION IF NOT EXISTS vector;
 *   CREATE TABLE code_embeddings (...);
 *   CREATE INDEX idx_embeddings_vector ON code_embeddings USING hnsw (embedding vector_cosine_ops);
 */
public class PgVectorStore implements VectorStore {

    private static final Logger logger = Logger.getLogger(PgVectorStore.class.getName());

    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final int vectorDimension;
    private Connection connection;

    public PgVectorStore(String jdbcUrl, String username, String password, int vectorDimension) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.vectorDimension = vectorDimension;
    }

    @Override
    public void initialize() {
        try {
            connect();
            createExtension();
            createTable();
            createIndex();
            logger.info("PGVector store initialized");
        } catch (Exception e) {
            logger.warning("PGVector init failed: " + e.getMessage());
        }
    }

    @Override
    public void upsert(RagSlice slice) {
        if (slice.getEmbedding() == null) return;
        try {
            String sql = "INSERT INTO code_embeddings " +
                    "(project_name, file_path, method_name, class_name, " +
                    "start_line, end_line, slice_code, token_count, slice_type, " +
                    "metadata_json, embedding) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::vector) " +
                    "ON CONFLICT (project_name, file_path, method_name, start_line) " +
                    "DO UPDATE SET embedding = EXCLUDED.embedding, " +
                    "              slice_code = EXCLUDED.slice_code, " +
                    "              metadata_json = EXCLUDED.metadata_json";

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, getProjectName(slice));
                ps.setString(2, slice.getFilePath());
                ps.setString(3, slice.getMethodName());
                ps.setString(4, slice.getClassName());
                ps.setInt(5, slice.getStartLine());
                ps.setInt(6, slice.getEndLine());
                ps.setString(7, slice.getCode());
                ps.setInt(8, slice.getTokenCount());
                ps.setString(9, slice.getType() != null ? slice.getType().name() : null);
                ps.setObject(10, toJson(slice.getMetadata()), Types.OTHER);
                ps.setString(11, vectorToString(slice.getEmbedding()));
                ps.executeUpdate();
            }
        } catch (Exception e) {
            logger.warning("Upsert failed: " + e.getMessage());
        }
    }

    @Override
    public void upsertBatch(List<RagSlice> slices) {
        for (RagSlice slice : slices) {
            upsert(slice);
        }
    }

    @Override
    public List<RagSlice> searchByVector(float[] queryVector, int topK) {
        List<RagSlice> results = new ArrayList<>();
        try {
            String sql = "SELECT project_name, file_path, method_name, class_name, " +
                    "start_line, end_line, slice_code, token_count, slice_type, metadata_json, " +
                    "1 - (embedding <=> ?::vector) AS similarity " +
                    "FROM code_embeddings " +
                    "ORDER BY embedding <=> ?::vector " +
                    "LIMIT ?";

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, vectorToString(queryVector));
                ps.setString(2, vectorToString(queryVector));
                ps.setInt(3, topK);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        RagSlice slice = new RagSlice();
                        slice.setClassName(rs.getString("class_name"));
                        slice.setFilePath(rs.getString("file_path"));
                        slice.setMethodName(rs.getString("method_name"));
                        slice.setStartLine(rs.getInt("start_line"));
                        slice.setEndLine(rs.getInt("end_line"));
                        slice.setCode(rs.getString("slice_code"));
                        slice.setTokenCount(rs.getInt("token_count"));
                        String typeStr = rs.getString("slice_type");
                        if (typeStr != null) slice.setType(RagSlice.SliceType.valueOf(typeStr));
                        results.add(slice);
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("Vector search failed: " + e.getMessage());
        }
        return results;
    }

    @Override
    public List<RagSlice> getByProject(String projectName) {
        List<RagSlice> results = new ArrayList<>();
        try {
            String sql = "SELECT * FROM code_embeddings WHERE project_name = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, projectName);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        RagSlice slice = new RagSlice();
                        slice.setClassName(rs.getString("class_name"));
                        slice.setFilePath(rs.getString("file_path"));
                        slice.setMethodName(rs.getString("method_name"));
                        slice.setStartLine(rs.getInt("start_line"));
                        slice.setEndLine(rs.getInt("end_line"));
                        slice.setCode(rs.getString("slice_code"));
                        slice.setTokenCount(rs.getInt("token_count"));
                        results.add(slice);
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("Query by project failed: " + e.getMessage());
        }
        return results;
    }

    @Override
    public void deleteByProject(String projectName) {
        try {
            String sql = "DELETE FROM code_embeddings WHERE project_name = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, projectName);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            logger.warning("Delete by project failed: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.warning("Close connection failed: " + e.getMessage());
        }
    }

    // ========== 内部方法 ==========

    private void connect() throws SQLException {
        connection = DriverManager.getConnection(jdbcUrl, username, password);
    }

    private void createExtension() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE EXTENSION IF NOT EXISTS vector");
        }
    }

    private void createTable() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS code_embeddings (" +
                    "id SERIAL PRIMARY KEY, " +
                    "project_name VARCHAR(255) NOT NULL, " +
                    "file_path VARCHAR(500) NOT NULL, " +
                    "method_name VARCHAR(200), " +
                    "class_name VARCHAR(300), " +
                    "start_line INTEGER, " +
                    "end_line INTEGER, " +
                    "slice_code TEXT, " +
                    "token_count INTEGER, " +
                    "slice_type VARCHAR(20), " +
                    "metadata_json JSONB, " +
                    "embedding vector(" + vectorDimension + "), " +
                    "created_at TIMESTAMP DEFAULT NOW(), " +
                    "UNIQUE (project_name, file_path, method_name, start_line)" +
                    ")");
        }
    }

    private void createIndex() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // HNSW 索引 (pgvector 0.5+)
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_embeddings_vector " +
                    "ON code_embeddings USING hnsw (embedding vector_cosine_ops) " +
                    "WITH (m = 16, ef_construction = 64)");
            // 项目名索引
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_embeddings_project " +
                    "ON code_embeddings (project_name)");
        }
    }

    private String vectorToString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    private String toJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":");
            Object val = entry.getValue();
            if (val instanceof String) sb.append("\"").append(val).append("\"");
            else sb.append(val);
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private String getProjectName(RagSlice slice) {
        if (slice.getMetadata() != null && slice.getMetadata().containsKey("project")) {
            return (String) slice.getMetadata().get("project");
        }
        // 从文件路径提取项目名
        String path = slice.getFilePath();
        int idx = path.indexOf("/src/main/java/");
        if (idx > 0) return path.substring(0, idx).replaceAll(".*/", "");
        return "default";
    }
}
