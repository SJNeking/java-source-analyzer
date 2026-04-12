package cn.dolphinmind.glossary.java.analyze.rag.store;

import cn.dolphinmind.glossary.java.analyze.rag.model.RagSlice;
import java.util.*;

/**
 * Vector Store 接口
 * 
 * 支持向量存储和相似度检索
 */
public interface VectorStore {

    /**
     * 初始化存储 (创建表/索引)
     */
    void initialize();

    /**
     * 存储切片及其向量
     */
    void upsert(RagSlice slice);

    /**
     * 批量存储
     */
    void upsertBatch(List<RagSlice> slices);

    /**
     * 向量相似度检索 (Top-K)
     * @return 按余弦相似度降序排列的切片列表
     */
    List<RagSlice> searchByVector(float[] queryVector, int topK);

    /**
     * 按项目名查询所有切片
     */
    List<RagSlice> getByProject(String projectName);

    /**
     * 删除项目相关数据
     */
    void deleteByProject(String projectName);

    /**
     * 关闭连接
     */
    void close();
}
