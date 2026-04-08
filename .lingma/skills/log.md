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

## ⚙️ 强制执行流程

### Step 1: 环境保活
```python
home_dir = os.environ.get('HOME', '/Users/mingxilv')
backend_path = f"{home_dir}/learn/s-pay-mall-ddd/.lingma/learning-log/backend"
run_in_terminal(f"lsof -i :8002 | grep LISTEN || (cd {backend_path} && nohup python3 main.py > /tmp/ll.log 2>&1 &)")
```

### Step 2: 构造数据 (结构化思维注入规范)

**核心原则**: 
1. **内容归内容，元数据归系统**: AI **严禁**在 JSON 中编造 `created_at` 或 `scan_time`。时间戳必须由 `auto_record.py` 在入库瞬间通过 `datetime.now()` 自动生成，以确保时间线的绝对权威性和一致性。
2. **思维维度承载**: 每一个 JSON 字段都必须承载《AI 助手决策框架》中的特定思维维度，严禁使用模糊的占位符。
3. **字数自检**: 在提交前，AI 必须自检 `insight` 长度是否达标（≥2500字）。

```json
{
  "topic": "精准概括：[领域] + [核心技术点] + [解决的问题]（例：DDD架构下支付状态机的幂等性设计）",
  
  "insight": "深度分析文章（≥2500字），必须包含以下四个模块：\n1. **Decision Trace (决策链路)**: 详细记录 Clarify(需求澄清), Investigate(工具调用与事实获取), Generate(方案空间), Evaluate(MCDA加权评分) 的全过程。\n2. **STAR Analysis**: 结合项目上下文，运用第一性原理剖析 Situation 和 Task，深度展开 Action 中的权衡取舍（Trade-offs）。\n3. **Domain Mapping**: 将技术决策映射到具体业务场景（如电商秒杀、金融清算），分析其边界条件。\n4. **Socratic Reflection**: 提出 1-2 个关于技术演进或架构哲学的深层反问。",
  
  "diagram": "Mermaid graph TD 流程图：必须展示从‘原始需求’到‘最终方案’的逻辑推导路径，节点需标注关键决策点（如：为何选择 Redis 而非 DB）。",
  
  "code_snippet": "完整可运行代码：必须包含类定义、关键方法实现及必要的注释。禁止提供伪代码或片段，需体现 macOS 风格的代码美学。",
  
  "star_situation": "背景锚定：描述当前的技术栈约束（如 Spring Boot 版本）、业务痛点（如并发超卖）及已有的失败尝试。",
  
  "star_task": "目标拆解：明确需要达成的量化指标（如 QPS 提升 50%）或定性目标（如消除循环依赖）。",
  
  "star_action": "行动推演：记录多方案对比过程。例如：方案 A（简单但扩展性差）vs 方案 B（复杂但高性能），并说明最终选择的理由。",
  
  "star_result": "价值闭环：总结方案实施后的收益，包括性能提升数据、代码可读性改善或对后续开发的指导意义。",
  
  "topic_tag_id": "cn.dolphinmind.learning.log.tag.discipline.cs.ai.skill",
  "project_tag_id": "null 或 具体的 project.component 标签ID",
  "research_type": "deep-research (深度研究) | topic-exploration (主题探索) | domain-mapping (领域映射)",
  "energy_level": 1-5 (根据任务复杂度与认知负荷自评),
  "aha_moment": true/false (是否产生了颠覆性的认知突破)"
}
```

#### 字段填充示例 (以本次重构为例):
*   **topic**: "Skill 规范演进：基于决策框架的思维显性化重构"
*   **insight**: "...在本次重构中，我通过 MCDA 模型评估了三种演进路径。其中‘增量升级’方案在技术可行性（30%权重）上得分最高，因为它保留了原有的入库脚本兼容性。为了达到 2500 字的要求，我进一步分析了人类认知与机器存储的本质差异，并引入了 CAPTURE Loop 协议来确保协作的严密性..."
*   **diagram**: `graph TD\n    A[旧规范: 禁言执行] --> B{重构决策}\n    B -->|引入框架| C[新规范: 思维显性化]`
*   **star_action**: "我并行读取了 `record.md` 和 `ai-decision-framework.md`，发现旧规范缺失了 Evaluate 环节。因此，我在新的 JSON 结构中强制加入了 `decision_trace` 的输出要求，并规定了每个子模块的最小字数限制..."

### Step 3: 入库 (带完整性验证)

**⚠️ 重要变更**: 不再直接传递JSON到命令行(会被Shell截断),改用文件+验证脚本方式。

```python
import json
import os

home_dir = os.environ.get('HOME', '/Users/mingxilv')

# 1. 将JSON写入临时文件(避免命令行长度限制)
tmp_file = "/tmp/log_entry.json"
with open(tmp_file, 'w', encoding='utf-8') as f:
    json.dump(payload, f, ensure_ascii=False, indent=2)

# 2. 使用增强版脚本保存(自动验证数据完整性)
script_path = f"{home_dir}/learn/java-source-analyzer/dev-ops/script/quick_save.py"
result = run_in_terminal(f"python3 {script_path} {tmp_file}", has_risk=False)

# 3. 检查验证结果
if "✅✅✅ 验证通过" in result:
    print("✅ 学习记录已成功保存并验证")
elif "❌ 验证失败" in result:
    print("❌ 数据完整性验证失败,请检查输出")
    # 显示实际保存的内容片段用于排查
else:
    print("⚠️ 无法确认保存状态,请手动检查数据库")
```

**验证机制说明:**
- `quick_save.py` 会自动查询数据库,对比预期长度vs实际长度
- 如果检测到数据截断,会显示差异和实际内容片段
- 只有验证通过才视为成功,否则必须重新保存

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
