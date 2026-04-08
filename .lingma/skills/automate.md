---
name: automation-orchestrator
aliases: [/auto, /generate-report, /learning-profile, /organize-knowledge]
description: 学习日志自动化编排器，负责周期性总结、画像分析与知识库维护。
---

# Learning Log Automation Orchestrator (v2.0)

## 🎯 核心目标

将碎片化的灵感记录转化为结构化的**知识资产**。通过自动化脚本定期扫描 `learning-log.db`，生成周报、分析学习趋势并维护标签体系。

---

## 📋 可用自动化指令

| 指令 | 功能描述 | 输出产物 |
| :--- | :--- | :--- |
| `/生成周报` | 汇总本周所有 STAR 记录，按主题聚类生成深度总结文章。 | Markdown 报告 + Mermaid 脉络图 |
| `/学习画像` | 统计研究类型分布、能量消耗趋势、高频标签云。 | 数据可视化图表 + 趋势分析 |
| `/每日复盘` | 随机抽取一条旧记录，引导用户进行间隔重复复习与补充。 | 交互式问答卡片 |
| `/整理标签` | 检测未分类或冲突的标签，提供合并/重命名建议。 | 标签优化建议列表 |

---

## ⚙️ 执行逻辑 (AI 协同模式)

### Step 1: 数据提取
运行 Python 脚本从 SQLite 数据库中提取指定时间范围（如最近 7 天）的记录：
```python
# 示例：查询本周记录
SELECT * FROM learning_entries WHERE timestamp >= date('now', '-7 days');
```

### Step 2: AI 聚合与分析
将提取的 JSON 数据喂给 AI，要求执行以下操作：
1. **主题聚类**：将相似的 `topic` 归为一类（如“并发安全”、“DDD 建模”）。
2. **结论升华**：基于多条记录的 `star_result`，提炼出更高层面的技术洞察。
3. **图示生成**：为本周的知识脉络生成一张 Mermaid `mindmap` 或 `graph TD`。

### Step 3: 持久化与反馈
1. 将生成的报告保存至 `.lingma/reports/weekly/YYYY-Www.md`。
2. 调用后端 API 将这份“周报”也作为一条特殊的 `summary` 类型记录存入数据库。
3. 在前端展示报告摘要。

---

## 🛠️ 脚本实现规范

所有自动化脚本位于 `.lingma/learning-log/scripts/`：

| 脚本名称 | 职责 |
| :--- | :--- |
| `generate_weekly_report.py` | 核心周报生成器，处理聚类和 Markdown 格式化。 |
| `analyze_learning_metrics.py` | 指标收集器，计算活跃度、专注度等维度。 |
| `cleanup_tags.py` | 标签清洗工具，处理孤儿标签和同义词。 |

---

## 🔄 调度方案 (可选)

虽然主要通过指令触发，但也支持后台自动运行：

### macOS Launchd 配置
若需每周日早 9 点自动生成草稿：
```xml
<!-- ~/Library/LaunchAgents/com.learninglog.weekly-report.plist -->
<key>StartCalendarInterval</key>
<dict>
    <key>Hour</key><integer>9</integer>
    <key>Minute</key><integer>0</integer>
    <key>Weekday</key><integer>0</integer>
</dict>
```

---

## 💡 使用建议

1. **先手动后自动**：建议先通过 `/生成周报` 熟悉报告结构，再考虑配置定时任务。
2. **闭环反馈**：生成的周报不仅是记录，更是你下周学习的起点（Action Items）。
3. **严谨性校验**：AI 在生成周报时，会自动检查原始记录是否缺失 STAR 字段或 Mermaid 图示，并给出提醒。
