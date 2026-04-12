package cn.dolphinmind.glossary.java.analyze.slicing;

import java.util.*;

/**
 * 代码切片 — 从 AST 精准提取的代码片段
 * 
 * 用于 RAG 检索和 AI 审查的输入
 */
public class CodeSlice {

    private String filePath;              // 文件路径
    private String className;             // 类全限定名
    private String methodName;            // 方法名 (null = 类级切片)
    private int startLine;                // 起始行
    private int endLine;                  // 结束行
    private String code;                  // 切片代码
    private int tokenCount;               // 预估 Token 数
    private SliceType type;               // 切片类型
    private List<String> dependencies;    // 方法依赖的外部类/方法
    private Map<String, Object> context;  // 静态分析上下文

    public enum SliceType {
        CLASS,        // 整个类
        METHOD,       // 单个方法
        ISSUE_AREA,   // 问题相关代码区域
    }

    public CodeSlice() {}

    /**
     * 预估 Token 数 (粗略: 1 token ≈ 4 chars for English, 2 chars for Chinese)
     */
    public int estimateTokenCount() {
        if (code == null) return 0;
        // 混合中英文的粗略估算
        int chineseChars = 0;
        int totalChars = code.length();
        for (int i = 0; i < totalChars; i++) {
            char c = code.charAt(i);
            if (c >= 0x4e00 && c <= 0x9fff) chineseChars++;
        }
        int englishChars = totalChars - chineseChars;
        return (chineseChars / 2) + (englishChars / 4);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("filePath", filePath);
        m.put("className", className);
        m.put("methodName", methodName);
        m.put("startLine", startLine);
        m.put("endLine", endLine);
        m.put("code", code);
        m.put("tokenCount", estimateTokenCount());
        m.put("type", type != null ? type.name() : null);
        if (dependencies != null && !dependencies.isEmpty()) m.put("dependencies", dependencies);
        if (context != null && !context.isEmpty()) m.put("context", context);
        return m;
    }

    // Getters & Setters
    public String getFilePath() { return filePath; }
    public void setFilePath(String fp) { this.filePath = fp; }
    public String getClassName() { return className; }
    public void setClassName(String cn) { this.className = cn; }
    public String getMethodName() { return methodName; }
    public void setMethodName(String mn) { this.methodName = mn; }
    public int getStartLine() { return startLine; }
    public void setStartLine(int sl) { this.startLine = sl; }
    public int getEndLine() { return endLine; }
    public void setEndLine(int el) { this.endLine = el; }
    public String getCode() { return code; }
    public void setCode(String c) { this.code = c; }
    public int getTokenCount() { return estimateTokenCount(); }
    public SliceType getType() { return type; }
    public void setType(SliceType t) { this.type = t; }
    public List<String> getDependencies() { return dependencies; }
    public void setDependencies(List<String> d) { this.dependencies = d; }
    public Map<String, Object> getContext() { return context; }
    public void setContext(Map<String, Object> ctx) { this.context = ctx; }
}
