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
        String sql = "SELECT id, project_name, source, raw_json, analysis_ms, created_at FROM analysis_results WHERE id = ?";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToDTO(rs));
                }
            }
        } catch (SQLException e) {
            logger.warning("Failed to find analysis result: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<AnalysisResultDTO> findByProjectName(String projectName, int limit) {
        List<AnalysisResultDTO> results = new ArrayList<>();
        String sql = "SELECT id, project_name, source, raw_json, analysis_ms, created_at FROM analysis_results WHERE project_name = ? LIMIT ?";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, projectName);
            ps.setInt(2, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRowToDTO(rs));
                }
            }
        } catch (SQLException e) {
            logger.warning("Failed to find analysis results by project name: " + e.getMessage());
        }
        return results;
    }

    @Override
    public void deleteOlderThan(long timestamp) {
        String sql = "DELETE FROM analysis_results WHERE created_at < to_timestamp(?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, timestamp);
            ps.executeUpdate();
            logger.info("Deleted old analysis results before timestamp: " + timestamp);
        } catch (SQLException e) {
            logger.warning("Failed to delete old results: " + e.getMessage());
        }
    }

    private AnalysisResultDTO mapRowToDTO(ResultSet rs) throws SQLException {
        AnalysisResultDTO dto = new AnalysisResultDTO();
        dto.setTaskId(String.valueOf(rs.getLong("id")));
        dto.setProjectName(rs.getString("project_name"));
        dto.setAnalysisTimeMs(rs.getLong("analysis_ms"));
        return dto;
    }
}
