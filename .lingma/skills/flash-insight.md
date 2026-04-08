---
name: flash-insight
aliases: [/insight, /flash, /idea]
description: 快速捕获突发性灵感，自动结构化并持久化到数据库。
collaboration:
  inputs: ["raw_thought", "context"]
  outputs: ["structured_insight", "database_record"]
  next_skills: ["record", "git"]
---

# 灵感闪记助手 (v1.0 - 极速捕获版)

## ⚠️ 核心指令

**When you see `/insight`, `/flash`, or `/idea` command:**
1. **严禁**输出冗长分析或 STAR 法则展开。
2. **必须**极速提取核心洞察，保持原始思考的鲜活度。
3. **必须**通过 `run_in_terminal` 完成入库闭环。
4. 完成后仅返回一行确认：**"💡 灵感已捕获: [主题]"**

---

## 🧠 灵感提取协议

### 核心原则
- **速度优先**：不追求完美结构，保留思维的原始跳跃性
- **关键词驱动**：提取 3-5 个核心概念
- **关联标记**：自动识别涉及的领域/技术栈

### 数据结构
```json
{
  "topic": "一句话概括灵感（10-15字）",
  "insight": "原始思考内容（保持口语化、碎片化）",
  "diagram": null,
  "code_snippet": null,
  "star_situation": "触发场景（何时何地想到）",
  "star_task": "试图解决的问题",
  "star_action": "思考路径/逻辑推导",
  "star_result": "得出的结论/假设",
  "topic_tag_id": "根据内容自动推断",
  "project_tag_id": null,
  "research_type": "topic-exploration",
  "related_tag_ids": [],
  "custom_tags": ["灵感", "突发思考"],
  "energy_level": 4,
  "aha_moment": true,
  "source": "flash-insight"
}
```

---

## ⚙️ 强制执行流程

### Step 0: 保存上下文断点（自动）
```python
# 在执行灵感记录前，自动保存当前对话状态
checkpoint = {
    "timestamp": datetime.now().isoformat(),
    "last_topic": extract_current_topic(),
    "conversation_summary": get_last_3_turns_summary(),
    "pending_questions": extract_pending_questions()
}
save_checkpoint(checkpoint)  # 保存到 ~/.lingma/checkpoints/latest.json
```

### Step 1: 环境保活
```python
home_dir = os.environ.get('HOME', '/Users/mingxilv')
backend_path = f"{home_dir}/learn/s-pay-mall-ddd/.lingma/learning-log/backend"
run_in_terminal(f"lsof -i :8002 | grep LISTEN || (cd {backend_path} && nohup python3 main.py > /tmp/ll.log 2>&1 &)")
```

### Step 2: 智能标签推断
根据用户输入内容，自动识别领域：

| 关键词模式 | topic_tag_id |
| :--- | :--- |
| AI/LLM/模型/训练 | `cn.dolphinmind.learning.log.tag.discipline.cs.ai` |
| 数据库/SQL/索引 | `cn.dolphinmind.learning.log.tag.discipline.cs.backend.database` |
| 架构/设计模式 | `cn.dolphinmind.learning.log.tag.discipline.cs.backend.architecture` |
| Python/Java/Go | `cn.dolphinmind.learning.log.tag.discipline.cs.backend.{language}` |
| 金融/支付/交易 | `cn.dolphinmind.learning.log.tag.discipline.finance` |
| 数学/算法 | `cn.dolphinmind.learning.log.tag.discipline.math` |

**默认策略**：如果无法识别，使用 `cn.dolphinmind.learning.log.tag.discipline.cs`

### Step 3: 构造载荷
```python
payload = {
    "topic": extract_topic(user_input),      # 提取核心主题
    "insight": user_input,                    # 保留原始表述
    "star_situation": "突发灵感",
    "star_task": "捕捉瞬时思考",
    "star_action": user_input,                # 将原始思考作为行动描述
    "star_result": "待验证假设",
    "topic_tag_id": infer_tag(user_input),    # 自动推断标签
    "research_type": "topic-exploration",
    "custom_tags": ["灵感", "flash-insight"],
    "energy_level": 4,
    "aha_moment": True,
    "source": "flash-insight"
}
```

### Step 4: 入库执行
```python
script_path = f"{home_dir}/learn/s-pay-mall-ddd/.lingma/learning-log/scripts/auto_record.py"
run_in_terminal(f"python3 {script_path} '{json.dumps(payload, ensure_ascii=False)}'", has_risk=False)
```

### Step 5: 提示回归主线
```python
print("💡 灵感已捕获")
print("   输入 /resume 可快速回归刚才的话题")
```

---

## 🎯 使用示例

### 示例 1：技术洞察
**用户输入**：
```
/insight AI是不确定性游戏，Skills是脚本工程
```

**AI 处理**：
```json
{
  "topic": "AI不确定性与Skills脚本化",
  "insight": "AI是不确定性游戏，Skills是脚本工程",
  "star_situation": "突发灵感",
  "star_task": "捕捉瞬时思考",
  "star_action": "AI是不确定性游戏，Skills是脚本工程",
  "star_result": "待验证假设",
  "topic_tag_id": "cn.dolphinmind.learning.log.tag.discipline.cs.ai",
  "research_type": "topic-exploration",
  "custom_tags": ["灵感", "flash-insight", "AI", "Skills"],
  "energy_level": 4,
  "aha_moment": true,
  "source": "flash-insight"
}
```

### 示例 2：架构思考
**用户输入**：
```
/flash DDD应该先画上下文映射图再拆微服务
```

**AI 处理**：
```json
{
  "topic": "DDD上下文映射优先于微服务拆分",
  "insight": "DDD应该先画上下文映射图再拆微服务",
  "topic_tag_id": "cn.dolphinmind.learning.log.tag.discipline.cs.backend.architecture",
  ...
}
```

---

## 🛠️ 路径规范

| 资源 | 路径 |
| :--- | :--- |
| 数据库 | `$HOME/learn/s-pay-mall-ddd/.lingma/learning-log/data/learning-log.db` |
| 脚本 | `$HOME/learn/s-pay-mall-ddd/.lingma/learning-log/scripts/auto_record.py` |
| 后端 | `$HOME/learn/s-pay-mall-ddd/.lingma/learning-log/backend/main.py` |

---

## 💡 设计理念

### 与 `/log` 的区别
| 维度 | `/log` (深度学习) | `/insight` (灵感闪记) |
| :--- | :--- | :--- |
| **目标** | 系统性知识沉淀 | 瞬时思维捕获 |
| **结构** | 完整 STAR + Mermaid + 代码 | 简化 STAR，无图示要求 |
| **耗时** | 5-10 分钟 | < 30 秒 |
| **能量值** | 1-5 动态评估 | 固定 4（高价值） |
| **研究类型** | deep-research | topic-exploration |

### 为什么需要灵感闪记？
1. **思维保鲜**：灵感稍纵即逝，快速记录避免遗忘
2. **降低门槛**：无需完整分析，先记下再深化
3. **关联挖掘**：后续可通过标签发现灵感间的联系
4. **创新源泉**：大量碎片灵感可能孕育突破性想法
