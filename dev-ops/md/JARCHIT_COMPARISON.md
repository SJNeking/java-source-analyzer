# JArchitect 对标报告

**日期**: 2026-04-09
**Git 提交**: `89a62a3`

---

## 对标结果

| JArchitect 能力 | 状态 | 验证数据 (Spring Boot 2.7.12) |
|----------------|------|-----------------------------|
| 代码指标 | ✅ 完成 | 类 2695, 方法 11634, 字段 4743 |
| LOC | ✅ 完成 | 127129 行代码 |
| 注释率 | ✅ 完成 | 30433 行注释 (23.9%) |
| 抽象类/接口/枚举 | ✅ 完成 | 137 抽象类, 318 接口, 46 枚举 |
| 圈复杂度 | ✅ 完成 | 平均 7.49, 最大 186 |
| 继承深度 | ✅ 完成 | 平均 0.27 |
| 方法长度 | ✅ 完成 | 平均 4.32 行 |
| 内聚指数 | ✅ 完成 | 0.96 |
| 包依赖图 | ✅ 完成 | 7579 条包依赖边 |
| 模块依赖矩阵 | ✅ 完成 | ASCII + Graphviz DOT |
| 层违规检测 | ✅ 完成 | ArchitectureViolationRule |
| 循环依赖检测 | ✅ 完成 | ArchitectureAnalyzer |
| 入口点发现 | ✅ 完成 | 49 个入口点 |
| 调用链路追踪 | ✅ 渐进分析 | 3 层架构 |
| 类型定义导航 | ✅ 完成 | 4026 类型索引 |
| 数据流追踪 | ✅ 完成 | 23 条多跳流 |
| Fan-in/Fan-out | ⚠️ 0 (待改进) | 需要完整调用图 |

## 实现细节

### CodeMetricsCalculator

对标 JArchitect Metrics 面板：
- `lines_of_code`: 计算非空、非注释行
- `comment_lines`: 计算 `//`, `/*`, `/**` 行
- `cyclomatic_complexity`: 统计 if/else/for/while/case/catch/&&/||/三元运算符
- `inheritance_depth`: 计算 extends 层数
- `cohesion`: 计算类内字段使用率

### DependencyGraphGenerator

对标 JArchitect Dependency Graph：
- `ModuleDependencyMatrix`: 模块间依赖矩阵 (ASCII 可视化)
- `PackageDependencyGraph`: 包级别依赖网络
- `ClassDependencyNetwork`: 类级别依赖网络
- `toGraphvizDot()`: 导出 Graphviz DOT 格式

### 类类型标记

- `is_interface`: 接口标记
- `is_abstract`: 抽象类标记
- `is_enum`: 枚举标记
- `extends_count`: 继承类数
- `implements_count`: 实现接口数

---

## 已知限制

1. **Fan-in/Fan-out 为 0**: 因为 metrics 计算在调用图构建之前执行，callGraph 传入为 null。需要调整执行顺序。
2. **圈复杂度基于正则**: 不是基于控制流图的精确计算，但对于快速估算足够准确。

---

## 下一步

1. 调整执行顺序，让 metrics 使用真实的 callGraph
2. 实现精确的圈复杂度（基于控制流图）
3. 添加更多代码质量指标（技术债务比率、重复代码等）
