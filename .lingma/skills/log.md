_---
name: log
aliases: [/record, /save-learning, /capture-insight]
description: "Deep analysis and archiving of technical insights using the 5-Step Decision Framework. Enforces ≥2500 words and full AI thought process."
collaboration:
  inputs: ["dialogue_context", "technical_insight", "workspace_manifest"]
  outputs: ["structured_entry", "decision_trace", "database_record"]
  triggers: [from_coach_analysis, from_ws_discovery]
  next_skills: ["git", "knowledge"]
---

# 深度学习归档助手 (v12.0 - 决策框架驱动版)

## ⚠️ 核心指令 (Zero-Tolerance for Laziness)

**When you see `/log` command, you must immediately enter "Execution Mode":**
1. **字数强制**: `insight` 字段内容**必须不少于 2500 字**。如果内容不足，AI 必须进行深度扩展（如增加第一性原理分析、多方案对比、领域映射等），严禁使用“简而言之”、“综上所述”等缩略语。
2. **思维显性化**: 必须在归档内容中包含完整的“AI 决策链路追踪”，展示从需求澄清到方案评估的全过程。
3. **结构化表达**: 严格遵循 STAR 法则，且每个部分都必须有实质性的技术细节支撑。
4. **视觉强制**: 必须包含 Mermaid 流程图（展示决策逻辑）和 ASCII Art（展示架构概览）。
5. **全闭环入库**: 通过 `run_in_terminal` 调用 `quick_save.py` 完成持久化，并自动执行端到端验证。
6. **严禁假执行**: 必须验证数据库中实际保存的内容长度与输入一致，否则视为失败。

---

## 📖 内容生成规范 (基于 spec-ai-decision-framework.md)

### 1. 五步决策流程记录 (Decision Trace) - 必填项
在生成的 `insight` 中，必须包含以下决策维度的详细记录，**每部分不得少于 200 字**：
- **Clarify (需求澄清)**: 识别用户的显性需求、隐性动机及隐含约束。分析用户为什么在此时提出此问题。
- **Investigate (信息收集)**: 记录并行调用的工具（如 `read_file`, `search_codebase`）及其获取的关键事实。说明为何选择这些工具而非其他。
- **Generate (方案生成)**: 描述产生的备选方案空间（最优、实用、快速、长远）。至少提供 3 种不同的解决思路。
- **Evaluate (方案评估)**: 展示多准则决策分析 (MCDA) 的加权评分过程。必须包含一个具体的评分表格。
- **Deliver (交付呈现)**: 说明如何根据用户偏好（如 macOS 风格、Mermaid 格式）调整最终输出。

### 2. 结构化表达 (STAR 法则增强版) - 必填项
- **Conclusion (结论先行)**: 开篇即给出核心洞察，直击本质。
- **Situation & Task**: 结合项目上下文，详细描述技术背景与任务目标。**禁止使用模糊的背景描述**。
- **Action**: 深度展开分析过程，融入第一性原理推导及领域映射（Domain Mapping）。**必须包含代码实现细节或架构权衡分析**。
- **Result**: 总结最终方案及其带来的量化收益或认知提升。**必须提供具体的数据或可验证的成果**。

### 3. 视觉化呈现 (Mandatory Visuals)
- **ASCII Art**: 必须使用符号化文本图表展示架构或流程概览。
- **Mermaid Flowchart**: 必须使用 `graph TD` 语法绘制详细逻辑流转图。

---

## ⚙️ 强制执行流程 (文件驱动模式)

### 核心原则: 思考与执行分离

**传统模式的问题:**
- AI通过`run_in_terminal`直接执行命令
- 存在Shell截断、网络失败等不确定性
- AI看到的是输出,但无法确认实际执行结果

**文件驱动模式的优势:**
- AI只负责生成思考内容,写入JSON文件
- 脚本读取文件并执行,逻辑完全确定
- 文件IO是原子操作,要么完整写入要么失败
- 脚本可以独立测试、审计、优化

### Step 1: AI生成思考内容并写入文件

AI必须完成以下任务:
1. 按照规范构造完整的JSON payload
2. 将JSON写入固定路径: `/tmp/log_entry_pending.json`
3. **不要调用任何保存脚本**,只负责写文件

```python
import json
import os

# AI生成的完整payload
payload = {
    "topic": "精准概括：[领域] + [核心技术点] + [解决的问题]",
    "insight": "深度分析文章（≥2500字）...",
    "diagram": "graph TD\n    A[开始] --> B[结束]",
    "code_snippet": "完整可运行代码...",
    "star_situation": "...",
    "star_task": "...",
    "star_action": "...",
    "star_result": "...",
    "topic_tag_id": "cn.dolphinmind.learning.log.tag.discipline.cs.ai.skill",
    "project_tag_id": None,  # Python的None,不是字符串"null"
    "research_type": "deep-research",
    "energy_level": 5,
    "aha_moment": True
}

# 写入待处理文件(原子操作)
pending_file = "/tmp/log_entry_pending.json"
with open(pending_file, 'w', encoding='utf-8') as f:
    json.dump(payload, f, ensure_ascii=False, indent=2)

print(f"✅ 思考内容已写入: {pending_file}")
print(f"   Insight长度: {len(payload['insight'])}字符")
print(f"   等待脚本处理...")
```

**关键约束:**
- ✅ AI只做: 生成内容 + 写文件
- ❌ AI不做: 调用保存脚本、检查数据库、验证结果

### Step 2: 用户或自动化触发脚本执行

**方式A: 手动执行(推荐用于调试)**
```bash
python3 /Users/mingxilv/learn/java-source-analyzer/dev-ops/script/process_log_entry.py
```

**方式B: 自动执行(生产环境)**
```python
# AI在写完文件后,可以提示用户执行
result = run_in_terminal(
    "python3 /Users/mingxilv/learn/java-source-analyzer/dev-ops/script/process_log_entry.py",
    has_risk=False
)
print(result)
```

### Step 3: 脚本执行并返回确定性结果

`process_log_entry.py`的职责:
1. 读取`/tmp/log_entry_pending.json`
2. 验证数据完整性(insight长度、必填字段等)
3. 调用API保存到数据库
4. 查询数据库验证实际保存的内容
5. 移动文件到归档目录: `/tmp/log_entry_archived/YYYYMMDD_HHMMSS.json`
6. 返回明确的执行结果(成功/失败+原因)

**脚本输出示例(成功):**
```
📖 读取待处理文件: /tmp/log_entry_pending.json
✅ 数据验证通过: insight长度=8619字符

💾 正在保存到数据库...
✅ API返回成功, ID: 22

🔍 验证数据库中实际内容...
   预期长度: 8619字符
   实际长度: 8619字符
✅ 验证通过!

📦 归档文件: /tmp/log_entry_archived/20260408_183000.json
✅✅✅ 学习记录已成功保存并验证 ✅✅✅
```

**脚本输出示例(失败):**
```
📖 读取待处理文件: /tmp/log_entry_pending.json
❌ 数据验证失败: insight长度=1800字符 < 2500字符要求

⚠️ 文件保留在: /tmp/log_entry_pending.json
请修正后重新执行脚本
```

---

## 🛠️ 路径规范

| 资源 | 路径 | 说明 |
| :--- | :--- | :--- |
| 数据库 | `$HOME/learn/s-pay-mall-ddd/.lingma/learning-log/data/learning-log.db` | SQLite存储 |
| **推荐脚本** | `$HOME/learn/java-source-analyzer/dev-ops/script/quick_save.py` | **带完整性验证,推荐使用** |
| 备用脚本 | `$HOME/learn/s-pay-mall-ddd/.lingma/learning-log/scripts/auto_record.py` | 原始脚本,无验证 |
| 后端服务 | `$HOME/learn/s-pay-mall-ddd/.lingma/learning-log/backend/main.py` | FastAPI服务 |
| 问题复盘文档 | `$HOME/learn/java-source-analyzer/dev-ops/script/PROBLEM_ANALYSIS_LOG_SAVING.md` | 根因分析 |
| 最佳实践指南 | `$HOME/learn/java-source-analyzer/dev-ops/script/README_LOG_SAVING.md` | 详细SOP |

## ⚠️ 常见错误与规避

### 错误1: 使用命令行参数传递大型JSON
```bash
# ❌ 错误: 会被Shell静默截断
python3 auto_record.py '{"insight":"8619字符..."}'

# ✅ 正确: 使用文件输入
python3 quick_save.py /tmp/log_entry.json
```

### 错误2: 不验证实际保存结果
```python
# ❌ 错误: 仅信任脚本输出
run_in_terminal("python3 auto_record.py ...")
print("保存成功")  # 可能是假的!

# ✅ 正确: 端到端验证
result = run_in_terminal("python3 quick_save.py ...")
if "验证通过" not in result:
    raise Exception("数据完整性验证失败")
```

### 错误3: insight长度不足2500字
- `quick_save.py` 会自动检测并警告
- 必须在提交前自检: `len(payload['insight']) >= 2500`
- 不足时应扩展深度分析,而非简化内容

## 📊 验证清单

每次执行 `/log` 后,必须确认:
- [ ] 使用了 `quick_save.py` 而非直接调用API
- [ ] JSON通过文件传递,而非命令行参数
- [ ] 看到 "✅✅✅ 验证通过! 数据完整保存 ✅✅✅" 消息
- [ ] 数据库中insight长度与输入一致(可手动抽查)
- [ ] insight长度≥2500字符
- [ ] 包含完整的Decision Trace五步流程
- [ ] 包含STAR四要素
- [ ] 包含Mermaid流程图和ASCII Art
