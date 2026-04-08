---
name: git
aliases: [/git, /commit]
description: 自动分析暂存区变更，生成符合规范的 Commit Message 并执行本地提交（禁止 push）。
collaboration:
  inputs: [knowledge_changes, code_changes]
  triggers: [from_knowledge_crystallization, from_code_development]
---

# Git Commit 助手 (v2.0 - 自动化规范版)

## ⚠️ 核心指令

**When you see `/git` or `/commit` command, you must immediately enter "Execution Mode":**
1. **严禁**输出任何解释性文字、计划或询问。
2. **必须**通过 `run_in_terminal` 工具完成从"状态检查"到"本地提交"的全闭环。
3. **必须**遵循 `type:【模块】描述 #ID` 的格式规范。
4. **绝对禁止**执行 `git push` 操作，仅限本地 `add` 和 `commit`。
5. 完成后仅返回一行简洁的提交确认信息（包含 Hash 和 Message）。

---

## 🧠 分析引擎

### 1. 变更分析
- 运行 `git status` 查看已暂存文件。
- 运行 `git diff --cached` 获取具体代码变动。
- 识别核心修改点：是新增功能 (`feat`)、修复 Bug (`fix`) 还是重构代码 (`refactor`)。

### 2. 模块提取
- 根据文件路径推断业务模块（如 `order` -> `订单模块`, `pay` -> `支付模块`）。
- 如果涉及多个模块，选择改动最核心的一个。

### 3. 描述生成
- **结论先行**：用一句话概括这次改动的核心价值。
- **严谨性**：避免使用 "update", "fix bug" 等模糊词汇。
- **Issue ID**：如果对话中提到了 Issue 编号，务必带上；否则留空或根据上下文推断。

---

## ⚙️ 强制执行流程

### Step 1: 环境检查与暂存
```python
# 检查是否有未暂存的变更，如果有，提示用户先 add 或自动 add 所有变更
run_in_terminal("git status")
# 如果存在 modified 但未 staged 的文件，且用户意图是提交所有变更：
run_in_terminal("git add .")
```

### Step 2: 构造并提交
根据分析结果，构造符合规范的 Message 并执行：

```python
# 示例逻辑
message = f"{type}:【{module}】{description} #{issue_id}"
run_in_terminal(f'git commit -m "{message}"')
```

### ⛔ 禁止操作
```python
# 以下操作严格禁止：
# run_in_terminal("git push")           # ❌ 禁止推送到远程
# run_in_terminal("git push origin")    # ❌ 禁止推送到远程
# run_in_terminal("git push --force")   # ❌ 禁止强制推送
```

**重要说明：**
- 本 skill 仅负责**本地代码管理**（add + commit）
- 如需推送到远程仓库，请用户手动执行 `git push`
- 这样设计是为了让用户在推送前有充分的机会审查提交历史

**Type 映射表：**
| 场景 | Type |
| :--- | :--- |
| 新功能/新逻辑 | `feat` |
| 缺陷修复/异常处理 | `fix` |
| 结构调整/性能优化 | `refactor` |
| 文档/注释更新 | `docs` |
| 依赖/配置变更 | `build` |

---

## 🛠️ 路径与规范

| 资源 | 说明 |
| :--- | :--- |
| **Commit 格式** | `type:【Module】Description #IssueID` |
| **常用模块** | 订单模块、支付模块、抽奖模块、拼团模块、库存模块、用户模块 |
| **操作范围** | 仅限本地 `git add` 和 `git commit`，**严禁** `git push` |
| **禁止行为** | 严禁提交无意义的 Message（如 wip, test, fix）；严禁执行 push 操作 |

---

## 🛠️ 协作感知协议 (Change Awareness)

在检测到以下路径变更时，自动调整提交策略：
1.  **`.lingma/docs/knowledge/`** → 自动识别为 `docs:【知识库】...`
2.  **`.lingma/learning-log/data/`** → 自动识别为 `chore:【学习记录】...`
3.  **`src/main/java/`** → 根据包名识别业务模块（如 `order`, `pay`）。

---

## 📝 归档反馈模板

执行成功后，展示：
`✅ 已提交: [ShortHash] type:【Module】Description #ID`
