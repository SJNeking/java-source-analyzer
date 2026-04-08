---
name: docwrite
aliases: [/doc, /write-doc, /create-doc]
description: 严格规范的文档编写助手，强制执行大小写规范和文件路径约束。
collaboration:
  inputs: ["doc_content", "doc_type"]
  outputs: ["formatted_doc", "correct_path"]
  next_skills: ["git", "knowledge"]
---

# 文档编写规范助手 (v1.0 - 严格约束版)

## ⚠️ 核心指令

**When you see `/doc`, `/write-doc`, or `/create-doc` command:**
1. **严禁**随意放置文档文件，必须遵循预设目录结构。
2. **严禁**忽略大小写规范，标题、文件名、路径必须严格符合约定。
3. **必须**在创建文件前验证路径合法性。
4. **必须**输出完整文件路径供用户确认。

---

## 📁 文件路径强制规范

### 文档分类与存储路径

| 文档类型 | 存储路径 | 命名规范 | 示例 |
| :--- | :--- | :--- | :--- |
| **ADR 架构决策** | `.lingma/docs/knowledge/adr/` | `ADR-XXX-Title.md` | `ADR-001-Lua-Scripting.md` |
| **Pitfall 踩坑记录** | `.lingma/docs/knowledge/pitfalls/` | `Pitfall-Topic.md` | `Pitfall-Concurrent-Modification.md` |
| **Pattern 设计模式** | `.lingma/docs/knowledge/patterns/` | `Pattern-Name.md` | `Pattern-Strategy-Payment.md` |
| **API 文档** | `.lingma/docs/api/` | `api-module-version.md` | `api-order-v2.md` |
| **开发指南** | `.lingma/docs/guides/` | `guide-topic.md` | `guide-deployment.md` |
| **技术规范** | `.lingma/docs/specs/` | `spec-category.md` | `spec-coding-style.md` |
| **会议纪要** | `.lingma/docs/meetings/` | `YYYY-MM-DD-topic.md` | `2026-04-07-architecture-review.md` |
| **Skill 文档** | `.lingma/skills/` | `skill-name.md` (单单词) | `flash-insight.md` |
| **学习记录** | `.lingma/learning-log/data/` | 由系统自动生成 | `records.json` |

### ❌ 禁止行为
```bash
# 错误示例
.lingma/docs/adr-001.md                    # ❌ 路径错误
.lingma/docs/knowledge/adr/adr-001-lua.md  # ❌ 文件名全小写
.lingma/docs/Knowledge/ADR-001.md          # ❌ 目录名大写
docs/ADR-001.md                            # ❌ 缺少 .lingma 前缀
.lingma/skills/Flash-Insight-Helper.md     # ❌ Skill 文件名非单单词
```

### ✅ 正确示例
```bash
# 正确示例
.lingma/docs/knowledge/adr/ADR-001-Lua-Scripting.md
.lingma/docs/knowledge/pitfalls/Pitfall-Concurrent-Modification.md
.lingma/docs/api/api-order-v2.md
.lingma/skills/flash-insight.md
```

---

## 🔤 大小写强制规范

### 1. 文件名规范

**ADR/Pitfall/Pattern 文档**：
- **格式**：`Type-XXX-Title-With-Hyphens.md`
- **规则**：
  - 类型前缀（ADR/Pitfall/Pattern）：**全大写**
  - 序号：**3位数字**，不足补零（001, 002, ...）
  - 标题部分：**每个单词首字母大写**（Title Case），用连字符分隔
  - 扩展名：**小写** `.md`

```
✅ ADR-001-Lua-Scripting.md
✅ Pitfall-Concurrent-Modification.md
✅ Pattern-Strategy-Payment.md

❌ adr-001-lua-scripting.md      # 全小写
❌ ADR-1-Lua-Scripting.md        # 序号未补零
❌ ADR-001-lua-scripting.md      # 标题全小写
❌ ADR-001_Lua_Scripting.md      # 使用下划线
```

**普通文档（API/Guides/Specs）**：
- **格式**：`category-topic-subtopic.md`
- **规则**：**全小写**，用连字符分隔

```
✅ api-order-v2.md
✅ guide-deployment.md
✅ spec-coding-style.md

❌ API-Order-V2.md               # 不应使用大写
❌ api_order_v2.md               # 不应使用下划线
```

**Skill 文件**：
- **格式**：`skill-name.md`（单单词或连字符分隔的小写单词）
- **规则**：**全小写**，优先单单词

```
✅ flash-insight.md
✅ resume-context.md
✅ git.md

❌ Flash-Insight.md              # 不应大写
❌ flash_insight.md              # 不应使用下划线
❌ flash-insight-helper.md       # 应避免冗长
```

### 2. 文档标题规范

**Markdown 一级标题（H1）**：
- **格式**：`# Type-XXX: Title With Spaces`
- **规则**：每个单词首字母大写（介词、冠词除外）

```markdown
✅ # ADR-001: Lua Scripting for Cache Management
✅ # Pitfall: Concurrent Modification Exception in HashMap
✅ # Pattern: Strategy Pattern for Payment Methods

❌ # adr-001: lua scripting                      # 全小写
❌ # ADR-001: LUA SCRIPTING                      # 全大写
❌ # ADR-001:lua scripting                       # 冒号后无空格
```

**二级及以下标题（H2-H6）**：
- **格式**：Sentence case（仅首单词首字母大写）或 Title Case
- **推荐**：保持一致性，全文统一使用一种风格

```markdown
✅ ## Background
✅ ## Decision
✅ ## Trade-offs

✅ ## Technical Implementation
✅ ## Performance Impact
```

### 3. 代码标识符规范

**类名/接口名**：PascalCase
```java
✅ OrderService
✅ PaymentStrategy
❌ orderService
❌ ORDER_SERVICE
```

**方法名/变量名**：camelCase
```java
✅ calculateTotal()
✅ orderId
❌ CalculateTotal()
❌ order_id
```

**常量**：UPPER_SNAKE_CASE
```java
✅ MAX_RETRY_COUNT
✅ DEFAULT_TIMEOUT_MS
❌ maxRetryCount
❌ Max_Retry_Count
```

---

## ⚙️ 强制执行流程

### Step 1: 识别文档类型
```python
def identify_doc_type(user_input):
    """根据用户输入识别文档类型"""
    keywords_map = {
        'adr': ['架构', '决策', '选型', 'adr', 'architecture'],
        'pitfall': ['踩坑', 'bug', '异常', 'pitfall', 'issue'],
        'pattern': ['模式', '设计', 'pattern', 'design'],
        'api': ['api', '接口', 'endpoint'],
        'guide': ['指南', '教程', 'guide', 'tutorial'],
        'spec': ['规范', '标准', 'spec', 'standard']
    }
    
    for doc_type, keywords in keywords_map.items():
        if any(kw in user_input.lower() for kw in keywords):
            return doc_type
    
    return 'guide'  # 默认类型
```

### Step 2: 生成合规路径
```python
def generate_doc_path(doc_type, title, seq_number=None):
    """生成符合规范的文档路径"""
    
    path_templates = {
        'adr': f'.lingma/docs/knowledge/adr/ADR-{seq_number:03d}-{to_title_case(title)}.md',
        'pitfall': f'.lingma/docs/knowledge/pitfalls/Pitfall-{to_title_case(title)}.md',
        'pattern': f'.lingma/docs/knowledge/patterns/Pattern-{to_title_case(title)}.md',
        'api': f'.lingma/docs/api/api-{to_kebab_case(title)}.md',
        'guide': f'.lingma/docs/guides/guide-{to_kebab_case(title)}.md',
        'spec': f'.lingma/docs/specs/spec-{to_kebab_case(title)}.md',
    }
    
    return path_templates.get(doc_type, f'.lingma/docs/guides/guide-{to_kebab_case(title)}.md')
```

### Step 3: 验证路径合法性
```python
def validate_path(filepath):
    """验证文件路径是否符合规范"""
    
    # 检查是否在允许的目录中
    allowed_dirs = [
        '.lingma/docs/knowledge/adr/',
        '.lingma/docs/knowledge/pitfalls/',
        '.lingma/docs/knowledge/patterns/',
        '.lingma/docs/api/',
        '.lingma/docs/guides/',
        '.lingma/docs/specs/',
        '.lingma/docs/meetings/',
        '.lingma/skills/'
    ]
    
    if not any(filepath.startswith(d) for d in allowed_dirs):
        raise ValueError(f"❌ 非法路径：{filepath}\n   必须在允许的目录中")
    
    # 检查大小写规范
    filename = os.path.basename(filepath)
    
    if filepath.startswith('.lingma/docs/knowledge/adr/'):
        if not re.match(r'^ADR-\d{3}-[A-Z][a-zA-Z-]+\.md$', filename):
            raise ValueError(f"❌ ADR 文件名格式错误：{filename}\n   应为：ADR-XXX-Title-Case.md")
    
    elif filepath.startswith('.lingma/skills/'):
        if not re.match(r'^[a-z][a-z-]*\.md$', filename):
            raise ValueError(f"❌ Skill 文件名格式错误：{filename}\n   应为：skill-name.md（全小写）")
    
    return True
```

### Step 4: 创建目录（如不存在）
```python
def ensure_directory_exists(filepath):
    """确保文件所在目录存在"""
    directory = os.path.dirname(filepath)
    os.makedirs(directory, exist_ok=True)
    print(f"✅ 目录已就绪：{directory}")
```

### Step 5: 生成文档内容
```python
def generate_doc_content(doc_type, title, content):
    """根据文档类型生成标准化内容"""
    
    templates = {
        'adr': generate_adr_template,
        'pitfall': generate_pitfall_template,
        'pattern': generate_pattern_template,
        'api': generate_api_template,
        'guide': generate_guide_template,
    }
    
    template_func = templates.get(doc_type, generate_guide_template)
    return template_func(title, content)
```

### Step 6: 输出确认信息
```python
print(f"📄 文档路径：{filepath}")
print(f"📝 文档类型：{doc_type.upper()}")
print(f"🔤 命名规范：{'✅ 符合' if validate else '❌ 不符合'}")
print(f"\n是否创建此文档？(y/n)")
```

---

## 📝 文档模板库

### ADR 模板
```markdown
# ADR-XXX: Title With Proper Casing

- **Status**: Accepted | Proposed | Deprecated | Superseded
- **Date**: YYYY-MM-DD
- **Authors**: [@username](link)
- **Context**: 
  Brief description of the problem and constraints.

- **Decision**: 
  Clear statement of the architectural decision.

- **Consequences**: 
  - ✅ Positive outcomes
  - ⚠️ Trade-offs and limitations

- **Diagram**:
  ```mermaid
  graph TD
    A[Before] --> B[After]
  ```

- **References**:
  - [[Related-Knowledge]]
  - [External Link](url)
```

### Pitfall 模板
```markdown
# Pitfall: Topic Name

- **Symptom**: 
  Description of the error or unexpected behavior.

- **Root Cause**: 
  Deep analysis using 5 Whys technique.

- **Solution**: 
  ```language
  // Before (Problematic)
  code snippet
  
  // After (Fixed)
  code snippet
  ```

- **Prevention**: 
  How to avoid this issue in the future.

- **Tags**: #pitfall #category #technology
```

### API 文档模板
```markdown
# API: Module Name vVersion

## Endpoint

```
METHOD /api/path/to/resource
```

## Request

### Headers
| Name | Type | Required | Description |
|------|------|----------|-------------|
| Authorization | string | Yes | Bearer token |

### Body
```json
{
  "field": "value"
}
```

## Response

### Success (200)
```json
{
  "data": {}
}
```

### Error (4xx/5xx)
```json
{
  "error": {
    "code": "ERROR_CODE",
    "message": "Description"
  }
}
```

## Examples

### cURL
```bash
curl -X POST ...
```

### JavaScript
```javascript
fetch(...)
```
```

---

## 🎯 使用示例

### 示例 1：创建 ADR 文档
**用户输入**：
```
/doc 使用 Lua 脚本优化缓存逻辑
```

**AI 处理**：
```python
doc_type = 'adr'
title = 'Lua Scripting For Cache Optimization'
seq_number = get_next_adr_number()  # 假设返回 5

filepath = '.lingma/docs/knowledge/adr/ADR-005-Lua-Scripting-For-Cache-Optimization.md'

# 验证
validate_path(filepath)  # ✅ 通过

# 输出
print("📄 文档路径：.lingma/docs/knowledge/adr/ADR-005-Lua-Scripting-For-Cache-Optimization.md")
print("📝 文档类型：ADR")
print("🔤 命名规范：✅ 符合")
```

### 示例 2：创建 Skill 文档
**用户输入**：
```
/doc 创建一个灵感闪记的 skill
```

**AI 处理**：
```python
doc_type = 'skill'
title = 'flash-insight'  # 单单词或连字符分隔的小写

filepath = '.lingma/skills/flash-insight.md'

# 验证
validate_path(filepath)  # ✅ 通过

# 输出
print("📄 文档路径：.lingma/skills/flash-insight.md")
print("📝 文档类型：SKILL")
print("🔤 命名规范：✅ 符合")
```

### 示例 3：错误路径拦截
**用户尝试**：
```python
filepath = '.lingma/docs/adr-001-lua.md'
validate_path(filepath)

# 输出
# ❌ 非法路径：.lingma/docs/adr-001-lua.md
#    必须在允许的目录中
#    正确路径：.lingma/docs/knowledge/adr/ADR-001-Lua.md
```

---

## 🛠️ 自动化检查工具

### Git Pre-commit Hook
```bash
#!/bin/bash
# .git/hooks/pre-commit

# 检查新增的 Markdown 文件
files=$(git diff --cached --name-only --diff-filter=A | grep '\.md$')

for file in $files; do
    # 检查是否在允许目录
    if [[ ! $file =~ ^\.lingma/(docs|skills)/ ]]; then
        echo "❌ 错误：文档必须放在 .lingma/docs/ 或 .lingma/skills/ 目录下"
        echo "   违规文件：$file"
        exit 1
    fi
    
    # 检查 ADR 命名规范
    if [[ $file =~ \.lingma/docs/knowledge/adr/ ]]; then
        if [[ ! $file =~ ADR-[0-9]{3}-[A-Z] ]]; then
            echo "❌ 错误：ADR 文件名必须符合 ADR-XXX-Title-Case.md 格式"
            echo "   违规文件：$file"
            exit 1
        fi
    fi
    
    # 检查 Skill 命名规范
    if [[ $file =~ \.lingma/skills/ ]]; then
        filename=$(basename $file)
        if [[ ! $filename =~ ^[a-z][a-z-]*\.md$ ]]; then
            echo "❌ 错误：Skill 文件名必须全小写，如 skill-name.md"
            echo "   违规文件：$file"
            exit 1
        fi
    fi
done

exit 0
```

---

## 💡 设计理念

### 为什么需要严格规范？
1. **可维护性**：统一的命名和路径让文档易于查找和管理
2. **专业性**：规范的文档体现团队的专业素养
3. **自动化**：标准化的格式便于工具自动处理和索引
4. **协作效率**：减少因命名混乱导致的沟通成本

### 常见错误及纠正
| 错误类型 | 错误示例 | 正确示例 | 原因 |
| :--- | :--- | :--- | :--- |
| 路径错误 | `docs/adr.md` | `.lingma/docs/knowledge/adr/ADR-001.md` | 必须使用标准目录 |
| 大小写混乱 | `Adr-001-lua.md` | `ADR-001-Lua.md` | 类型前缀大写，标题驼峰 |
| 序号不规范 | `ADR-1-Lua.md` | `ADR-001-Lua.md` | 3位数字补零 |
| 分隔符错误 | `ADR_001_Lua.md` | `ADR-001-Lua.md` | 使用连字符而非下划线 |
| Skill 命名 | `Flash-Insight.md` | `flash-insight.md` | Skill 全小写 |

---

## 📊 效果评估

### 成功指标
- **路径合规率**：100% 的文档在正确目录
- **命名合规率**：100% 的文件名符合大小写规范
- **检索效率**：文档查找时间 < 5 秒
- **维护成本**：减少 80% 的文档整理工作

### 持续改进
1. **定期检查**：每月运行一次文档规范检查脚本
2. **自动修复**：提供工具自动重命名不合规文件
3. **团队培训**：新成员入职时进行文档规范培训
4. **反馈机制**：收集规范执行中的问题并优化
