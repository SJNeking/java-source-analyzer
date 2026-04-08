---
name: resume-context
aliases: [/resume, /back, /continue]
description: 从灵感闪记中断点快速回归主线对话，恢复上下文连续性。
collaboration:
  inputs: ["interrupted_context", "flash_insight_record"]
  outputs: ["context_restoration", "conversation_continuity"]
  next_skills: ["record", "knowledge"]
---

# 上下文恢复助手 (v1.0 - 无缝回归版)

## ⚠️ 核心指令

**When you see `/resume`, `/back`, or `/continue` command:**
1. **严禁**重新解释或总结之前的对话。
2. **必须**精准定位到中断前的最后一个话题节点。
3. **必须**用一句话确认恢复点，然后立即继续。
4. 完成后仅返回：**"🔄 已回归主线：[最后话题]"**

---

## 🧠 上下文追踪协议

### 核心机制
- **断点标记**：在 `/insight` 执行前自动保存当前对话状态
- **栈式管理**：支持多层嵌套中断（灵感→灵感→回归）
- **语义锚点**：提取最后 3 轮对话的核心概念作为恢复锚点

---

## ⚙️ 强制执行流程

### Step 1: 检索最近断点
```python
checkpoint = load_latest_checkpoint()
if not checkpoint:
    return "⚠️ 未找到中断记录，请手动描述想继续的话题"
```

### Step 2: 恢复对话状态
```python
print(f"🔄 已回归主线：{checkpoint['last_topic']}")

if checkpoint.get('conversation_summary'):
    print(f"\n📝 上次进度：\n{checkpoint['conversation_summary']}")

if checkpoint.get('pending_questions'):
    print(f"\n💡 待回答问题：")
    for q in checkpoint['pending_questions']:
        print(f"   - {q}")
```

### Step 3: 清理断点记录
```python
clear_latest_checkpoint()
```

---

## 🎯 使用场景

### 场景 1：单层中断
```
用户：DDD 中聚合根的设计原则是什么？
AI：聚合根需要保证边界内的一致性...
用户：/insight 突然想到，聚合根其实像微服务的边界
AI：💡 灵感已捕获: 聚合根与微服务边界类比
用户：/resume
AI：🔄 已回归主线：DDD聚合根设计原则

继续刚才的内容，聚合根的第二个原则是...
```

### 场景 2：多层嵌套
```
用户：讲解 Spring 事务传播机制
AI：PROPAGATION_REQUIRED 表示...
用户：/flash 事务隔离级别和传播机制的关系？
AI：💡 灵感已捕获
用户：/flash 这个可以用状态机建模
AI：💡 灵感已捕获
用户：/resume
AI：🔄 已回归主线：Spring事务传播机制
```

---

## 🛠️ 技术实现

### 断点存储
```python
import json
import os

CHECKPOINT_DIR = os.path.expanduser("~/.lingma/checkpoints")

def save_checkpoint(conversation_state):
    checkpoint = {
        "id": f"{int(datetime.now().timestamp())}_{os.getpid()}",
        "timestamp": datetime.now().isoformat(),
        "state": conversation_state
    }
    
    filepath = os.path.join(CHECKPOINT_DIR, "latest.json")
    with open(filepath, 'w', encoding='utf-8') as f:
        json.dump(checkpoint, f, ensure_ascii=False, indent=2)
```

### 恢复函数
```python
def resume_conversation():
    checkpoint = load_latest_checkpoint()
    if not checkpoint:
        return "⚠️ 未找到中断记录"
    
    message = f"🔄 已回归主线：{checkpoint['last_topic']}\n"
    clear_latest_checkpoint()
    return message
```

---

## 💡 设计理念

### 为什么需要上下文恢复？
1. **思维连续性**：灵感闪记不应打断深度思考的心流状态
2. **认知负荷**：避免用户手动回忆"刚才说到哪了"
3. **效率提升**：秒级回归 vs 手动翻聊天记录
4. **体验优化**：让 AI 对话更像人类助手的自然交互

### 与浏览器的区别
| 维度 | 浏览器后退 | `/resume` |
| :--- | :--- | :--- |
| **粒度** | 整页跳转 | 语义级恢复 |
| **智能度** | 机械回退 | 理解上下文 |
| **灵活性** | 线性历史 | 支持分支回归 |
| **持久化** | 会话级 | 可跨会话恢复 |
