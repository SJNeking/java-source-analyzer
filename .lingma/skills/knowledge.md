---
name: knowledge
aliases: [/crystallize, /save-knowledge, /generate-adr]
description: "Automated knowledge crystallization. Enforces deep trade-off analysis and MCDA in ADRs. Prohibits shallow documentation."
collaboration:
  inputs: [adr_content, star_r_story, mermaid_diagram, decision_trace]
  triggers: [from_coach_analysis, from_log_learning]
  next_skills: [log, git]
---

# Knowledge Crystallization Assistant (v3.0 - Anti-Lazy Edition)

## 🎯 核心目标

实现从“代码实现”到“智慧资产”的自动化转化。**严禁生成流水账式的文档**，每一张知识卡片都必须包含深度的权衡分析（Trade-offs）和决策链路追踪。

## ⚠️ 严禁偷懒协议 (Anti-Laziness Protocol)
1. **禁止无脑记录**: 严禁只记录“做了什么”，必须解释“为什么这么做”以及“放弃了什么”。
2. **强制 MCDA 评估**: 在 ADR 中必须展示多准则决策分析的加权评分过程。
3. **思维显性化**: 必须包含 `Decision Trace` 模块，记录 AI 是如何从原始需求推导出最终架构决策的。
4. **量化收益**: 严禁使用“性能提升”等模糊词汇，必须提供具体的指标对比（如 QPS、RT、资源占用率）。

---

## ⚙️ 强制执行流程 (The CAPTURE Loop)

When user inputs `/crystallize [topic/feature]`, AI must execute:

### Step 1: 识别类型 (Identify Type)
**判断**：本次结晶属于哪一类？
- **ADR (架构决策)**: 涉及技术选型、边界划分或核心逻辑变更。
- **Pitfall (踩坑记录)**: 解决了 Bug、性能瓶颈或并发问题。
- **Pattern (设计模式)**: 发现并应用了某种通用解决方案。
- **Story (面试故事)**: 具有显著业务价值和技术深度的完整案例。

### Step 2: 提取核心要素 (Extract with Depth)
根据类型提取关键字段，**拒绝浅层信息**：
- **Context**: 为什么要做这个决定？痛点是什么？当时的约束条件有哪些？
- **Decision/Solution**: 最终选择了什么方案？（提供关键代码片段及 Mermaid 架构图）
- **Trade-offs (MCDA)**: 放弃了什么？换来了什么？必须进行加权评分。
- **Impact**: 带来了哪些量化指标的提升？
- **Decision Trace**: 记录 AI 辅助决策时的思维推导路径。

### Step 3: 结构化输出 (Structure)
按照预设模板生成 Markdown 内容：
- **图示化**: 强制使用 Mermaid 绘制架构图或流程图。
- **双向链接**: 自动建议关联的旧知识点（如 `[[相关模块]]`）。
- **标签体系**: 自动打标（如 `#adr`, `#domain/order`, `#pitfall/concurrency`）。

### Step 4: 归档建议 (Archive)
**输出**：建议的文件路径及文件名。
- 示例：`.lingma/docs/knowledge/adr/ADR-004-Lua-Scripting.md`

---

## 📝 标准化模板库 (深度版)

### 1. ADR 模板 (Architecture Decision Record)
```markdown
# ADR-[序号]: [决策标题]
- **状态**: Accepted
- **日期**: YYYY-MM-DD
- **背景 (Context)**: [一句话描述痛点及业务约束]
- **决策 (Decision)**: [核心方案及关键代码/配置]
- **权衡分析 (MCDA Trade-offs)**:
  | 维度 | 权重 | 方案 A 得分 | 方案 B 得分 | 理由 |
  | :--- | :--- | :--- | :--- | :--- |
  | 性能 | 40% | 9 | 6 | Redis 读写更快 |
  | 一致性 | 30% | 6 | 9 | DB 强一致 |
  | **总分** | **100%** | **7.8** | **7.5** | **选择方案 A** |
- **思维链路 (Decision Trace)**: [记录 AI 如何澄清需求并排除备选方案的过程]
- **图示**: 
  ```mermaid
  graph TD
    A[Before] -->|Decision| B[After]
  ```
```

### 2. Pitfall 模板 (Lessons Learned)
```markdown
# [踩坑名称]
- **现象**: [报错信息或异常表现]
- **根因 (5 Whys)**: [通过 5 个为什么挖掘到的本质原因]
- **修复**: [关键代码对比及修复逻辑]
- **防范**: [如何通过自动化测试或监控避免再次发生]
- **思维反思**: [当时为什么会忽略这个问题？认知盲区在哪？]
```

### 3. STAR-R 故事模板 (Interview Prep)
```markdown
# [故事标题]
- **S (情境)**: [业务背景及面临的极端约束]
- **T (任务)**: [需要达成的量化指标]
- **A (行动)**: [采取的技术手段，重点突出决策权衡过程]
- **R (结果)**: [量化收益，如 QPS 提升 50%]
- **R (反思)**: [如果重来会怎么做？有哪些未竟的优化点？]
```

---

## 🛠️ 路径与规范

| 资源 | 说明 |
| :--- | :--- |
| **存储路径** | `.lingma/docs/knowledge/` (按类型分子目录) |
| **命名规范** | `ADR-XXX-Title.md`, `Pitfall-Topic.md` |
| **关联机制** | 新生成的卡片必须包含至少一个指向现有知识的 `[[Link]]` |

---

## 🛠️ 协作接收协议 (Collaboration Protocol)

当接收到来自 `coach` 或 `log` 的产出时：
1.  **自动分类**: 根据内容识别是“架构决策 (ADR)”还是“面试故事 (STAR-R)”。
2.  **格式化入库**:
    *   ADR → 存入 `.lingma/docs/knowledge/adr/`
    *   STAR-R → 存入 `.lingma/learning-log/data/records.json`
3.  **触发归档**: 调用 `/git` 提交变更。
