# /log 命令快速执行指南

## 🚀 标准流程(3步完成)

### Step 1: 构造JSON数据

```python
payload = {
    "topic": "精准概括：[领域] + [核心技术点] + [解决的问题]",
    "insight": "深度分析文章（≥2500字）...",  # 必须包含Decision Trace + STAR
    "diagram": "graph TD\n    A[开始] --> B[结束]",
    "code_snippet": "完整可运行代码...",
    "star_situation": "...",
    "star_task": "...",
    "star_action": "...",
    "star_result": "...",
    "topic_tag_id": "cn.dolphinmind.learning.log.tag.discipline.cs.ai.skill",
    "project_tag_id": null,
    "research_type": "deep-research",
    "energy_level": 5,
    "aha_moment": true
}
```

### Step 2: 写入临时文件

```python
import json

tmp_file = "/tmp/log_entry.json"
with open(tmp_file, 'w', encoding='utf-8') as f:
    json.dump(payload, f, ensure_ascii=False, indent=2)

print(f"✅ JSON文件已创建: {len(payload['insight'])}字符")
```

### Step 3: 调用验证脚本保存

```python
result = run_in_terminal(
    "python3 /Users/mingxilv/learn/java-source-analyzer/dev-ops/script/quick_save.py /tmp/log_entry.json",
    has_risk=False
)

if "✅✅✅ 验证通过" in result:
    print("✅ 学习记录已成功保存并验证")
else:
    print("❌ 保存失败,请检查输出")
    print(result)
```

---

## ✅ 成功标志

看到以下输出表示保存成功:

```
📝 主题: AI时代代码质量确定性控制引擎战略定位
📏 Insight长度: 8619字符

💾 正在保存...
✅ API返回成功, ID: 20

🔍 验证数据库中实际内容...
   预期长度: 8619字符
   实际长度: 8619字符

✅✅✅ 验证通过! 数据完整保存 ✅✅✅
```

---

## ❌ 失败处理

### 情况1: insight长度不足

```
⚠️  警告: insight不足2500字符!
是否继续? (y/N):
```

**解决:** 扩展深度分析内容,不要简化!

### 情况2: 数据被截断

```
❌❌❌ 验证失败! 数据被截断 ❌❌❌
   差异: 8603字符丢失
   
   实际保存的内容:
   深度分析内容超过2500字......
```

**解决:** 
1. 确认使用了文件输入而非命令行参数
2. 检查JSON文件格式是否正确
3. 重新执行Step 2和Step 3

### 情况3: API返回错误

```
❌ 保存失败: 400
{"detail":"Project tag not found: null"}
```

**解决:** 
- `project_tag_id` 应该是Python的`None`,不是字符串`"null"`
- 或者使用具体的项目标签ID

---

## 🔍 手动验证(可选)

```bash
# 查询最新记录
sqlite3 ~/learn/s-pay-mall-ddd/.lingma/learning-log/data/learning-log.db \
  "SELECT id, topic, LENGTH(insight), timestamp FROM learning_entries ORDER BY id DESC LIMIT 1;"

# 查看所有短于2500字的记录(应该为空)
sqlite3 ~/learn/s-pay-mall-ddd/.lingma/learning-log/data/learning-log.db \
  "SELECT id, topic, LENGTH(insight) as len FROM learning_entries WHERE len < 2500 ORDER BY len ASC;"
```

---

## 📋 检查清单

执行前自检:
- [ ] insight长度≥2500字符
- [ ] 包含完整的Decision Trace五步流程
- [ ] 包含STAR四要素(situation/task/action/result)
- [ ] diagram以`graph TD`或`flowchart TD`开头
- [ ] code_snippet是完整可运行代码
- [ ] topic_tag_id存在且有效

执行后验证:
- [ ] 看到"✅✅✅ 验证通过"消息
- [ ] 数据库中insight长度与输入一致
- [ ] 能够查询到完整的主题和内容

---

## 🛠️ 工具位置

| 工具 | 路径 |
|------|------|
| **推荐脚本** | `/Users/mingxilv/learn/java-source-analyzer/dev-ops/script/quick_save.py` |
| Skill定义 | `/Users/mingxilv/learn/java-source-analyzer/.lingma/skills/log.md` |
| 问题复盘 | `/Users/mingxilv/learn/java-source-analyzer/dev-ops/script/PROBLEM_ANALYSIS_LOG_SAVING.md` |
| 最佳实践 | `/Users/mingxilv/learn/java-source-analyzer/dev-ops/script/README_LOG_SAVING.md` |

---

## ⚠️ 严禁行为

1. ❌ **严禁**直接通过命令行参数传递大型JSON
   ```bash
   # 错误! 会被Shell截断
   python3 auto_record.py '{"insight":"8619字符..."}'
   ```

2. ❌ **严禁**不验证就认为保存成功
   ```python
   # 错误! 可能是假的成功
   run_in_terminal("python3 auto_record.py ...")
   print("保存成功")
   ```

3. ❌ **严禁**使用简化版占位符
   ```python
   # 错误! 违反核心原则
   "insight": "深度分析内容超过2500字..."
   ```

---

## 💡 核心原则

> **"永远不要信任单一来源的成功消息,必须端到端验证数据完整性。"**

**三个永远:**
1. 永远用文件输入而非命令行参数
2. 永远在保存后验证数据库实际内容
3. 永远确保insight≥2500字且不简化

---

**最后更新:** 2026-04-08  
**版本:** v2.0 (带完整性验证)
