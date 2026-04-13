package cn.dolphinmind.glossary.java.analyze.repository.impl;

import cn.dolphinmind.glossary.java.analyze.dto.AnalysisResultDTO;
import cn.dolphinmind.glossary.java.analyze.repository.AnalysisRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * AnalysisRepository 的 JDBC 实现
 */
public class AnalysisRepositoryImpl implements AnalysisRepository {

    private static final Logger logger = Logger.getLogger(AnalysisRepositoryImpl.class.getName());

    @Override
    public void save(AnalysisResultDTO dto) {
        String sql = "INSERT INTO analysis_results " +
                "(project_name, source, raw_json, analysis_ms) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setString(1, dto.getProjectName());
            ps.setString(2, "unified");
            ps.setString(3, "{}"); // Placeholder for raw JSON
            ps.setLong(4, dto.getAnalysisTimeMs());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    dto.setTaskId(String.valueOf(rs.getLong(1)));
                }
            }
            logger.info("Saved analysis result for: " + dto.getProjectName());
            
        } catch (SQLException e) {
            logger.warning("Failed to save analysis result: " + e.getMessage());
        }
    }

    @Override
    public Optional<AnalysisResultDTO> findById(String id) {
        // Implementation omitted for brevity in this draft
        return Optional.empty();
    }

    @Override
    public List<AnalysisResultDTO> findByProjectName(String projectName, int limit) {
        List<AnalysisResultDTO> results = new ArrayList<>();
        String sql = "SELECT id, project_name, created_at FROM analysis_results WHERE project_name = ? LIMIT ?";
        
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, projectName);
            ps.setInt(2, limit);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AnalysisResultDTO dto = new AnalysisResultDTO();
                    dto.setTaskId(String.valueOf(rs.getLong("id")));
                    dto.setProjectName(rs.getString("project_name"));
                    results.add(dto);
                }
            }
        } catch (SQLException e) {
            logger.warning("Failed to query analysis results: " + e.getMessage());
        }
        return results;
    }

    @Override
    public void deleteOlderThan(long timestamp) {
        // Implementation omitted
    }
}
