package cn.dolphinmind.glossary.java.analyze.rag.search;

import cn.dolphinmind.glossary.java.analyze.rag.model.RagSlice;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * BM25 关键词检索 (基于 PostgreSQL 全文检索)
 * 
 * 使用 tsvector/tsquery 实现高效的关键词匹配
 */
public class Bm25Searcher {

    private static final Logger logger = Logger.getLogger(Bm25Searcher.class.getName());

    private final String jdbcUrl;
    private final String username;
    private final String password;
    private Connection connection;

    public Bm25Searcher(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    /**
     * 无参构造 (内存模式, 不执行数据库操作)
     */
    public Bm25Searcher() {
        this.jdbcUrl = null;
        this.username = null;
        this.password = null;
    }

    /**
     * 初始化 (添加 tsvector 列和 GIN 索引)
     */
    public void initialize() {
        if (jdbcUrl == null) {
            logger.info("BM25 searcher skipped (in-memory mode)");
            return;
        }
        try {
            connection = DriverManager.getConnection(jdbcUrl, username, password);
            createSearchColumn();
            createIndex();
            logger.info("BM25 searcher initialized");
        } catch (Exception e) {
            logger.warning("BM25 init failed: " + e.getMessage());
        }
    }

    /**
     * 关键词检索 (Top-K)
     */
    public List<RagSlice> search(String query, int topK) {
        if (jdbcUrl == null) return Collections.emptyList();
        List<RagSlice> results = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) return results;

        try {
            // PostgreSQL tsquery 格式
            String tsQuery = query.toLowerCase()
                    .replaceAll("[^a-z0-9_\\s]", " ")
                    .trim()
                    .replaceAll("\\s+", " & ");

            String sql = "SELECT project_name, file_path, method_name, class_name, " +
                    "start_line, end_line, slice_code, token_count, slice_type, metadata_json, " +
                    "ts_rank(search_tsv, to_tsquery('simple', ?)) AS rank " +
                    "FROM code_embeddings " +
                    "WHERE search_tsv @@ to_tsquery('simple', ?) " +
                    "ORDER BY rank DESC " +
                    "LIMIT ?";

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, tsQuery);
                ps.setString(2, tsQuery);
                ps.setInt(3, topK);

                try (ResultSet rs = ps.executeQuery()) {
                    int rank = 0;
                    while (rs.next()) {
                        rank++;
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
                        
                        // 计算 BM25 分数 (简化版: 使用 ts_rank)
                        double bm25Score = rs.getDouble("rank");
                        slice.setBm25Score(bm25Score);
                        
                        results.add(slice);
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("BM25 search failed: " + e.getMessage());
        }
        return results;
    }

    /**
     * 关闭连接
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.warning("Close BM25 connection failed: " + e.getMessage());
        }
    }

    // ========== 内部方法 ==========

    private void createSearchColumn() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // 添加 tsvector 列 (如果不存在)
            stmt.execute("ALTER TABLE code_embeddings ADD COLUMN IF NOT EXISTS search_tsv tsvector");
            // 填充已有数据
            stmt.execute("UPDATE code_embeddings SET search_tsv = " +
                    "to_tsvector('simple', COALESCE(class_name, '') || ' ' || " +
                    "COALESCE(method_name, '') || ' ' || " +
                    "COALESCE(slice_code, '')) " +
                    "WHERE search_tsv IS NULL");
            // 触发器自动更新
            stmt.execute("CREATE OR REPLACE FUNCTION update_search_tsv() RETURNS trigger AS $$ " +
                    "BEGIN " +
                    "NEW.search_tsv := to_tsvector('simple', " +
                    "COALESCE(NEW.class_name, '') || ' ' || " +
                    "COALESCE(NEW.method_name, '') || ' ' || " +
                    "COALESCE(NEW.slice_code, '')); " +
                    "RETURN NEW; " +
                    "END; " +
                    "$$ LANGUAGE plpgsql");
            stmt.execute("DROP TRIGGER IF EXISTS tsvector_update ON code_embeddings");
            stmt.execute("CREATE TRIGGER tsvector_update BEFORE INSERT OR UPDATE " +
                    "ON code_embeddings FOR EACH ROW EXECUTE FUNCTION update_search_tsv()");
        }
    }

    private void createIndex() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_embeddings_tsv " +
                    "ON code_embeddings USING GIN (search_tsv)");
        }
    }
}
