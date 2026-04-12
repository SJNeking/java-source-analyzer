package cn.dolphinmind.glossary.java.analyze.rag.llm;

import cn.dolphinmind.glossary.java.analyze.rag.model.RagSlice;
import cn.dolphinmind.glossary.java.analyze.unified.UnifiedIssue;
import java.util.*;

/**
 * LLM Client 接口
 * 
 * 用于调用大语言模型进行代码审查
 */
public interface LlmClient {

    /**
     * 调用 LLM 生成审查结果
     * @param prompt 系统+用户提示
     * @return LLM 响应文本
     */
    String chat(String prompt);

    /**
     * 调用 LLM 生成结构化审查结果
     * @param context 上下文 (代码切片 + 静态分析结果)
     * @param prompt 提示词
     * @return 结构化审查问题列表
     */
    List<UnifiedIssue> reviewCode(List<RagSlice> context, String prompt);

    /**
     * 模型名称
     */
    String getModelName();
}
