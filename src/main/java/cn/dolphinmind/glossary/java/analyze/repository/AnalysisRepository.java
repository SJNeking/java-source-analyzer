package cn.dolphinmind.glossary.java.analyze.repository;

import cn.dolphinmind.glossary.java.analyze.dto.AnalysisResultDTO;

import java.util.List;
import java.util.Optional;

/**
 * 分析结果仓储接口
 * 
 * 定义了对 analysis_results 表的 CRUD 操作。
 */
public interface AnalysisRepository {

    /**
     * 保存分析结果记录
     */
    void save(AnalysisResultDTO dto);

    /**
     * 根据 ID 查询
     */
    Optional<AnalysisResultDTO> findById(String id);

    /**
     * 查询项目的历史分析记录
     */
    List<AnalysisResultDTO> findByProjectName(String projectName, int limit);

    /**
     * 删除旧记录
     */
    void deleteOlderThan(long timestamp);
}
