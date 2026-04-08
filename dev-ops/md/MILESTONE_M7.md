# 里程碑 M7: AnalysisOrchestrator + 消除硬编码

**日期**: 2026-04-09
**Git 提交**: `ed1c3f8`

---

## 一、架构重构

### 1.1 AnalysisOrchestrator 流程编排

创建 `orchestrate` 包，将配置和执行分离：

```
orchestrate/
├── AnalysisConfig.java          # 配置对象（替代散落的局部变量）
├── AnalysisOrchestrator.java    # 流程编排器（协调各服务）
├── AnalysisPipeline.java        # 流水线阶段（预留，逐步迁移）
└── ScanResult.java              # 扫描结果中间对象
```

### 1.2 硬编码消除

| 之前 | 之后 |
|------|------|
| `main()` 内大量局部变量 | `AnalysisConfig` 统一承载 |
| 默认路径硬编码在 main | `config.applyDefaults()` 集中管理 |
| 输出路径硬编码 | `config.getOutputDirPath()` |
| 缓存路径硬编码 | `config.getCacheDir()` |

### 1.3 代码精简

- 删除 ~500 行重复代码
- `main()` 简化为 4 行：解析参数 → 创建服务 → 创建编排器 → 执行
- `runAnalysis(AnalysisConfig, SemanticTranslator)` 成为可编程入口

---

## 二、修复

### 2.1 CrossMethodTaintAnalyzer ClassCastException

- `method.get("body_code")` 可能返回非 String 类型
- 增加 `instanceof String` 检查

---

## 三、验证

HikariCP 5.0.1 完整分析：
- 63 类, 1529 方法, 281 字段
- 注释覆盖率 53.85%
- Quality Gate: PASSED
- 所有报告正常生成
