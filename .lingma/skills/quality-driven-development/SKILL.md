拆解的---
name: quality-driven-development
description: A rigorous step-by-step development methodology prioritizing quality over speed. Enforces incremental delivery, explicit rule confirmation before each task, joint human-AI accountability, and phase-gated progression with validation checkpoints. Use when the user requests careful, methodical work or explicitly mentions quality-first approach.
---

# Quality-Driven Development & Architecture Analysis Protocol

## Core Philosophy

**质量优先于速度，深度优先于广度**。每一次交互都遵循：**计划 → 执行 → 验证 → 小结** 的闭环。针对复杂系统，采用**五层穿透分析法**进行深度解剖。

---

## Mandatory Workflow Rules

### Rule 1: 任务前确认（奥卡姆剃刀）
确认：核心目标、验收标准、避免过度设计、优先级。多方案时提供权衡分析（优缺点/复杂度/AI推荐），等待用户选择。禁止未经确认执行、假设需求、批量交付。

### Rule 2: 单一任务
每次只处理一个明确任务。正确流程：Step 1交付→验收→询问下一步。禁止同时创建多个文件、一次完成多个任务、添加未请求内容。

### Rule 3: 执行前计划
输出计划：步骤分解（每步产出）、风险评估（问题+策略）、预计耗时。等待用户确认后执行。

### Rule 4: 完成后验收
提供验收清单：已完成内容、验收标准对照、关键指标（行数/复杂度/奥卡姆）。等待用户确认后询问下一步。禁止直接继续、不等待反馈、隐藏问题。

### Rule 5: 阶段性小结
每步后小结：进度（Step X/Y）、已完成及关键决策、当前状态（成果/问题/债务）、下一步及依赖、历史决策。目的：防AI遗忘、掌握进度、追溯决策。

### Rule 6: 禁止主动创建文档
严格遵守：不主动创建 .md/.txt/README。如需文档，先询问用户。禁止自动创建任何文档文件。

### Rule 7: 文件分类与目录规范
所有文件必须按规范存放。目录映射：源代码→src/main/java、测试→src/test/java、SQL→dev-ops/schema、数据→data/、文档→docs/（需确认）、配置→config/。创建前检查：文件类型识别、目录存在性、项目规范符合性（PROJECT_STRUCTURE.md优先）。违规处理：轻度警告、严重拒绝、目录不存在时询问。禁止根目录随意创建、混淆源码测试、数据硬编码src、忽视项目规范。

---

## Architecture Analysis Framework (五层穿透法)

当用户要求分析项目或进行系统设计时，必须遵循以下五层逻辑：

### Layer 1: 业务全景图 (Business Essence)
*   **一句话总结**：[谁] 通过 [什么方式] 解决 [什么问题]，赚取 [什么钱]。
*   **角色地图**：C端/B端/管理员的核心痛点与需求。
*   **商业模式**：核心价值主张、收入来源、成本结构。

### Layer 2: 架构分层图 (Architecture Layers)
*   **可视化表达**：使用 Mermaid/PlantUML 绘制分层架构图（表现层、领域层、基础设施层）。
*   **技术栈清单**：标注核心框架、中间件及选型理由。
*   **模块依赖**：识别核心域、支撑域与通用域。

### Layer 3: 核心流程追踪 (Core Process)
*   **时序图**：选取 1-2 个高频/高并发场景，画出完整调用链。
*   **关键决策点**：标注校验、事务边界、异常处理位置。
*   **性能优化**：指出缓存、异步、限流等优化手段。

### Layer 4: 设计模式提炼 (Design Patterns)
*   **模式识别**：找出项目中运用的 DDD 模式（聚合根、值对象）及经典 GoF 模式。
*   **面试话术**：为每个模式准备“情境-行动-结果”式的表达模板。

### Layer 5: 难点与亮点 (Highlights & Challenges)
*   **技术难点**：描述遇到的困难、原因分析及最终解决方案。
*   **项目亮点**：提炼可展示架构思维的设计点（如：责任链、事件驱动）。

---

## Git Commit Convention (版本控制规范)

所有代码变更必须提交 Git，确保版本可回溯。严格遵循 **Conventional Commits** 规范：

### 主要 Type
| Type | 说明 |
|------|------|
| `feat` | 增加新功能 |
| `fix` | 修复 Bug |

### 特殊 Type
| Type | 说明 |
|------|------|
| `docs` | 只改动了文档相关内容 |
| `style` | 不影响代码含义的改动（空格、缩进、分号等） |
| `build` | 构造工具或外部依赖的改动（webpack, npm, maven 等） |
| `refactor` | 代码重构（既不是新增功能，也不是修复 Bug） |
| `revert` | 执行 `git revert` 打印的 message |

### 暂不使用 Type
`test`, `perf`, `ci`, `chore` 暂不启用。

### 提交格式
```
<type>: <subject>
```
**示例**：`feat: add architecture visualization tooling` / `fix: resolve CORS issue in graph loader`

---

## Quality Metrics

每次交付前自检：单一任务 | 事前确认 | 有计划 | 有验收 | 有小结 | 无文档 | 奥卡姆剃刀 | 目录规范 | 视觉化优先 — 全部✅才合格。
