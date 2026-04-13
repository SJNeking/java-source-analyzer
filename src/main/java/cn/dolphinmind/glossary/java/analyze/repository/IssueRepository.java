package cn.dolphinmind.glossary.java.analyze.repository;

import cn.dolphinmind.glossary.java.analyze.dto.IssueDTO;

import java.util.List;

/**
 * 代码问题仓储接口
 * 
 * 定义了对 unified_issues 表的操作。
 */
public interface IssueRepository {

    /**
     * 批量保存问题
     */
    void batchSave(String analysisId, List<IssueDTO> issues);

    /**
     * 根据分析 ID 查询所有问题
     */
    List<IssueDTO> findByAnalysisId(String analysisId);

    /**
     * 按严重程度过滤查询
     */
    List<IssueDTO> findByAnalysisIdAndSeverity(String analysisId, String severity);

    /**
     * 统计某次分析的问题数量
     */
    int countByAnalysisId(String analysisId);
}
