package cn.dolphinmind.glossary.java.analyze.repository.impl;

import cn.dolphinmind.glossary.java.analyze.dto.IssueDTO;
import cn.dolphinmind.glossary.java.analyze.repository.IssueRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * IssueRepository 的 JDBC 实现
 */
public class IssueRepositoryImpl implements IssueRepository {

    private static final Logger logger = Logger.getLogger(IssueRepositoryImpl.class.getName());

    @Override
    public void batchSave(String analysisId, List<IssueDTO> issues) {
        if (issues == null || issues.isEmpty()) return;

        String sql = "INSERT INTO unified_issues " +
                "(result_id, source, severity, category, message, file_path, line) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            for (IssueDTO issue : issues) {
                ps.setString(1, analysisId);
                ps.setString(2, issue.getSource());
                ps.setString(3, issue.getSeverity());
                ps.setString(4, issue.getCategory());
                ps.setString(5, issue.getMessage());
                ps.setString(6, issue.getFilePath());
                ps.setInt(7, issue.getLine());
                ps.addBatch();
            }
            ps.executeBatch();
            conn.commit();
            logger.info("Batch saved " + issues.size() + " issues for analysis " + analysisId);

        } catch (SQLException e) {
            logger.warning("Failed to batch save issues: " + e.getMessage());
        }
    }

    @Override
    public List<IssueDTO> findByAnalysisId(String analysisId) {
        List<IssueDTO> issues = new ArrayList<>();
        String sql = "SELECT * FROM unified_issues WHERE result_id = ?";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, analysisId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    issues.add(mapRowToIssue(rs));
                }
            }
        } catch (SQLException e) {
            logger.warning("Failed to query issues: " + e.getMessage());
        }
        return issues;
    }

    @Override
    public List<IssueDTO> findByAnalysisIdAndSeverity(String analysisId, String severity) {
        // Implementation omitted
        return findByAnalysisId(analysisId);
    }

    @Override
    public int countByAnalysisId(String analysisId) {
        return 0;
    }

    private IssueDTO mapRowToIssue(ResultSet rs) throws SQLException {
        IssueDTO issue = new IssueDTO();
        issue.setId(String.valueOf(rs.getLong("id")));
        issue.setSource(rs.getString("source"));
        issue.setSeverity(rs.getString("severity"));
        issue.setCategory(rs.getString("category"));
        issue.setMessage(rs.getString("message"));
        issue.setFilePath(rs.getString("file_path"));
        issue.setLine(rs.getInt("line"));
        return issue;
    }
}
