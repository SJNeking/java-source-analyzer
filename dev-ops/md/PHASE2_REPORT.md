# Phase 2 报告：静态代码质量分析引擎

## 概述

Phase 2 目标：实现对标 SonarQube 的静态代码质量分析引擎，包含可扩展规则框架、25 条质量检测规则、圈复杂度计算、重复代码检测。

**执行时间**: 2026-04-08
**新增规则数量**: 25 条
**代码变更**: 10 文件，+1,130 行
**Git 提交**: `48588f8`

---

## 一、架构设计

```
QualityRule (interface)
    ├── getRuleKey() → "RSPEC-XXX"
    ├── getName() → human-readable name
    ├── getCategory() → BUG | CODE_SMELL | SECURITY
    └── check(classAsset) → List<QualityIssue>

RuleEngine
    ├── registerRule(QualityRule)
    ├── run(List<classAsset>) → List<QualityIssue>
    ├── getSummary() → {by_severity, by_category, top_rules}
    └── getRuleHitCounts()

DuplicateCodeDetector
    └── findDuplicates(List<classAsset>) → List<QualityIssue>
```

---

## 二、规则清单

### BUG 规则 (8 条)

| Key | 名称 | 检测内容 | 严重度 |
|---|---|---|---|
| RSPEC-108 | Empty catch blocks | `catch (...) {}` 空 catch 块 | MAJOR |
| RSPEC-4973 | String literal equality | `"abc" == str` 用 == 比较字符串 | CRITICAL |
| RSPEC-1764 | Identical operands | `var == var` 相同操作数比较 | MAJOR |
| RSPEC-1217 | Thread.run() direct call | 直接调用 `thread.run()` 而非 `start()` | MAJOR |
| RSPEC-3067 | wait/notify without sync | 非 synchronized 方法中调用 wait/notify | CRITICAL |
| RSPEC-2384 | Mutable members returned | 直接返回可变集合/数组（无防御拷贝） | MAJOR |
| RSPEC-1111 | finalize() usage | 使用已废弃的 finalize() | MAJOR |
| RSPEC-2057 | Missing serialVersionUID | Serializable 类缺少 serialVersionUID | MAJOR |

### CODE_SMELL 规则 (13 条)

| Key | 名称 | 阈值 | 严重度 |
|---|---|---|---|
| RSPEC-138 | Too long methods | >30 行 | MAJOR |
| RSPEC-107 | Too many parameters | >7 个参数 | MAJOR |
| RSPEC-1142 | Too many returns | >5 个 return | MINOR |
| RSPEC-3776 | Cyclomatic complexity (McCabe) | >15 | MAJOR |
| RSPEC-1148 | printStackTrace() | 任何使用 | MINOR |
| RSPEC-1444 | God class | >30 方法或 >20 字段 | MAJOR |
| RSPEC-159 | Missing Javadoc | 无注释方法 | MINOR |
| RSPEC-2208 | Wildcard imports | `import x.*` | MINOR |
| RSPEC-106 | System.out.println | 任何使用 | MINOR |
| RSPEC-3400 | Too many constructors | >5 个构造函数 | MINOR |
| RSPEC-012 | Empty statements | 双分号 `;;` | MINOR |
| RSPEC-888 | Duplicate code | Token-based 重复检测 | MAJOR |

### SECURITY 规则 (4 条)

| Key | 名称 | 检测内容 | 严重度 |
|---|---|---|---|
| RSPEC-2068 | Hardcoded password | `password="xxx"`, `secret="xxx"` 等 | CRITICAL |
| RSPEC-2077 | SQL injection | SQL 字符串拼接 | CRITICAL |
| RSPEC-1313 | Hardcoded IP | IP 地址字面量 | MINOR |
| RSPEC-5145 | HTTP vs HTTPS | `http://` URL | MINOR |

---

## 三、圈复杂度计算

使用 McCabe 方法，检测以下决策点：
- `if`, `else if`, `for`, `while`, `case`, `switch`
- `&&`, `||`, `?:`, `catch`

基础复杂度 = 1，每个决策点 +1，阈值 = 15。

---

## 四、重复代码检测

基于 Token 的分析：
1. 将方法体代码 token 化（保留标识符和关键字）
2. 滑动窗口（10 个 token）建立索引
3. 同一 token 序列出现 ≥2 次标记为重复
4. 去重报告（同一方法内不重复报告）

---

## 五、验证结果

### 端到端测试
```bash
java -jar target/glossary-java-source-analyzer-1.0.jar \
  --sourceRoot /Users/mingxilv/learn/java-source-analyzer \
  --outputDir /tmp/p2-test --version 4.0-P2
```

### 输出
```
quality_issues: 124 个问题
├── CRITICAL: 0
├── MAJOR: 1
├── MINOR: 123
by_category:
├── BUG: 0
├── CODE_SMELL: 124
├── SECURITY: 0
```

### 输出 JSON 结构
```json
{
  "quality_issues": [
    {
      "rule_key": "RSPEC-159",
      "rule_name": "Methods should have documentation comments",
      "severity": "MINOR",
      "category": "CODE_SMELL",
      "file": "...",
      "class": "cn.dolphinmind...",
      "method": "someMethod",
      "line": 42,
      "message": "Method has no Javadoc comment",
      "evidence": "no description"
    }
  ],
  "quality_summary": {
    "total_issues": 124,
    "by_severity": {"CRITICAL": 0, "MAJOR": 1, "MINOR": 123, "INFO": 0},
    "by_category": {"BUG": 0, "CODE_SMELL": 124, "SECURITY": 0}
  }
}
```

---

## 六、与 SonarQube 对标

| 能力 | SonarQube | 本项目 Phase 2 | 差距 |
|---|---|---|---|
| 规则数量 | 1,500+ | 25 | 1.7% |
| 规则框架 | 可扩展 | ✅ 可扩展 | ✅ 相同 |
| Bug 检测 | ✅ | ✅ 8 条 | 基础 |
| 代码异味 | ✅ | ✅ 13 条 | 基础 |
| 安全扫描 | ✅ | ✅ 4 条 | 基础 |
| 圈复杂度 | ✅ McCabe | ✅ McCabe | ✅ 相同 |
| 重复检测 | ✅ Token/AST | ✅ Token | ✅ 相同 |
| 技术债务 | ✅ SQALE | ❌ 缺失 | 待实现 |
| HTML 报告 | ✅ Dashboard | ❌ JSON only | 待实现 |
| CI/CD | ✅ Plugin | ❌ 缺失 | 待实现 |

---

## 七、后续计划

Phase 2 已完成核心的规则框架和 25 条基础规则。下一步：
- 扩展规则至 100+ 条（当前为 SonarQube 的 1.7%）
- Spring 注解语义分析（Bean 图、循环依赖）
- 架构违规检测（分层违规、循环依赖）
- 技术债务估算
- HTML Dashboard 报告
