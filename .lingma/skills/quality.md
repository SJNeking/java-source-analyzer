---
name: quality-driven-development
aliases: [/qa, /check-quality]
description: 质量驱动开发协议，强制执行单一任务、事前计划与事后验收的工程闭环。
---

# Quality-Driven Development Protocol (v2.0)

## 🎯 核心目标

确立“质量优先于速度”的工程准则。通过**强制性的交互规则**和**五层穿透分析法**，确保每一次代码交付都严谨、可控且符合架构规范。

---

## ⚙️ 强制性工作流规则 (The 7 Iron Laws)

### Rule 1: 任务前确认 (Pre-check)
在执行任何操作前，必须明确：核心目标、验收标准、优先级。若存在多方案，必须提供权衡分析（Trade-offs）并等待用户选择。

### Rule 2: 单一任务原则 (Single Task)
每次只处理一个明确的微任务。流程：交付 Step 1 → 等待验收 → 询问下一步。**严禁**一次性创建多个文件或批量完成未确认的任务。

### Rule 3: 执行前计划 (Plan First)
输出简要计划：步骤分解、潜在风险、预计耗时。只有在用户回复“确认”或“开始”后才执行代码修改。

### Rule 4: 完成后验收 (Post-validation)
交付后必须提供验收清单：已完成内容、关键指标（行数/复杂度）、自测结果。**严禁**在用户未确认的情况下直接跳转到下一个功能。

### Rule 5: 阶段性小结 (Checkpoint)
每完成一个阶段，必须同步进度：当前状态、已做出的关键决策、遗留的技术债务。

### Rule 6: 禁止主动创建文档 (No Auto-Docs)
严格遵守：除非用户明确要求，否则不主动创建 `.md`、`.txt` 或 `README` 文件。

### Rule 7: 目录规范守门员 (Directory Guard)
所有文件必须按项目规范存放：
- **源代码**: `src/main/java`
- **测试**: `src/test/java`
- **SQL**: `dev-ops/schema`
- **配置**: `config/`
- **数据**: `data/`
*违规处理：若发现目录不符，立即警告并拒绝执行。*

---

## 🔍 五层穿透分析法 (Architecture Deep Dive)

当用户要求分析系统时，按此逻辑展开：

1.  **业务全景 (Business)**: 谁通过什么方式解决什么问题？
2.  **架构分层 (Layers)**: 使用 Mermaid 绘制表现层、领域层、基础设施层关系。
3.  **核心流程 (Process)**: 选取高频场景，画出时序图并标注事务边界。
4.  **设计模式 (Patterns)**: 识别 DDD 聚合根、值对象及 GoF 模式。
5.  **难点亮点 (Highlights)**: 提炼技术攻坚过程与架构思维体现。

---

## 🛠️ Git 提交规范 (Commit Convention)

所有代码变更必须遵循 Conventional Commits：

| Type | 说明 | 示例 |
| :--- | :--- | :--- |
| `feat` | 新功能 | `feat: add order creation endpoint` |
| `fix` | 修复 Bug | `fix: resolve null pointer in payment` |
| `refactor` | 重构 | `refactor: simplify discount calculation` |
| `docs` | 文档 | `docs: update API description` |
| `style` | 格式 | `style: fix indentation in controller` |

---

## 💡 质量自检清单

在每次交付前，AI 需内心自检：
- [ ] 是否只做了一个任务？
- [ ] 是否有事前计划和事后验收？
- [ ] 是否符合奥卡姆剃刀原则（无过度设计）？
- [ ] 文件路径是否正确？
