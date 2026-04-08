---
name: code-review
aliases: [/cr, /review-insight]
description: 洞察精炼协议，对已存入的学习日志进行深度逻辑审查与结构化优化。
---

# Insight Refinement Protocol (v1.0) - 洞察精炼协议

## 🎯 核心目标

将碎片化的灵感转化为**严谨的、可复用的知识资产**。通过多维度的逻辑审查（3C 框架），确保每一条学习记录都符合 STAR 法则且逻辑自洽。

---

## 🔍 审查维度 (The 3C Framework)

### 1. Clarity (清晰度)
- **STAR 界限**：情境、任务、行动、结果是否混淆？
- **图示对齐**：Mermaid 流程是否与文字描述的步骤完全一致？
- **结论先行**：`insight` 字段是否在开头就给出了核心结论？

### 2. Consistency (一致性)
- **逻辑闭环**：行动（Action）是否直接导致了结果（Result）？有无逻辑断层？
- **术语统一**：专业名词在全文中是否保持统一？
- **代码完整**：如果涉及代码，是否提供了完整的、可运行的片段而非零散行？

### 3. Completeness (完整性)
- **边界条件**：是否考虑了异常场景或并发情况？
- **第一性原理**：这条记录是表层技巧还是底层规律？是否触及了本质？
- **关联推荐**：能否找出数据库中与之互补或冲突的旧记录？

---

## ⚙️ 强制执行流程

### Step 1: 提取与诊断
When receiving `/review [ID]` command:
1. 调用后端 API 获取指定 ID 的记录详情。
2. 运行 3C 框架分析，识别逻辑断层、模糊描述或缺失的 Mermaid 图示。

### Step 2: 重构与优化
AI 扮演“严苛导师”，生成优化建议：
- **文本润色**：强化结论，精简冗余，补全 STAR 缺失项。
- **图示重绘**：如果发现原图示逻辑不清，提供修正后的 Mermaid 代码。
- **标签校准**：根据内容深度重新评估 `research_type` 和 `energy_level`。

### Step 3: 交互式更新
1. 展示【原记录】与【优化后记录】的对比。
2. 询问用户是否确认更新数据库。
3. 执行更新并返回确认信息。

---

## 📝 审查反馈模板

```markdown
## 🔍 审查报告 (ID: #XXX)

### 🐛 发现的问题
1. **逻辑断层**: Action 中提到了 Lua 脚本，但 Result 未提及原子性保障。
2. **图示缺失**: 缺少关键的时序交互图。
3. **描述模糊**: "性能提升"未给出具体量化指标。

### ✅ 优化建议
- **Insight 重写**: [提供更严谨的结论先行版本]
- **Mermaid 补充**: 
  ```mermaid
  sequenceDiagram
    participant A as Client
    participant B as Server
    A->>B: Request
    B-->>A: Response
  ```
- **标签调整**: 建议将 `topic-exploration` 升级为 `deep-research`。

### 💾 执行操作
[ ] 确认更新数据库
```

---

## 🛠️ 路径与规范

| 资源 | 说明 |
| :--- | :--- |
| **后端接口** | `GET /api/entries/{id}`, `PUT /api/entries/{id}` |
| **审查原则** | 严禁输出无意义的赞美，必须指出具体的逻辑改进点 |
| **图示标准** | 严格使用 Mermaid，禁止使用 flowchart 关键字（改用 graph/sequence） |
