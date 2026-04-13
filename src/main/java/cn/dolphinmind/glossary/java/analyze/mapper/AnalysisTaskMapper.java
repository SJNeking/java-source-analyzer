package cn.dolphinmind.glossary.java.analyze.mapper;

import cn.dolphinmind.glossary.java.analyze.domain.AnalysisTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 分析任务 Mapper 接口
 */
@Mapper
public interface AnalysisTaskMapper {

    /**
     * 插入新的分析任务
     */
    int insert(AnalysisTask task);

    /**
     * 根据 ID 查询任务
     */
    AnalysisTask selectById(@Param("id") Long id);

    /**
     * 查询项目历史任务
     */
    List<AnalysisTask> selectByProjectName(@Param("projectName") String projectName, @Param("limit") int limit);

    /**
     * 删除旧任务
     */
    int deleteOlderThan(@Param("timestamp") long timestamp);
}
